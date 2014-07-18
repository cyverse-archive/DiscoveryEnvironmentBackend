(ns dewey.indexing
  "This is the logic for making changes to search index."
  (:require [clojurewerkz.elastisch.query :as es-query]
            [clojurewerkz.elastisch.rest.document :as es-doc]
            [clojure-commons.file-utils :as file]
            [dewey.doc-prep :as prep]
            [dewey.entity :as entity])
  (:import [java.util Map]
           [clojure.lang Keyword]))


(def ^{:private true} index "data")

(def ^{:private true} collection-type "folder")
(def ^{:private true} data-object-type "file")


(defmulti ^{:private true} mapping-type-of type)

(defmethod mapping-type-of Map
  [entity]
  (mapping-type-of (entity/entity-type entity)))

(defmethod mapping-type-of Keyword
  [entity-type]
  (case entity-type
    :collection  collection-type
    :data-object data-object-type))


(defn- index-doc
  [es mapping-type doc]
  (es-doc/create es index mapping-type doc :id (str (:id doc))))


(defn- update-doc
  [es entity script values]
  (es-doc/update-with-script es
                             index
                             (mapping-type-of entity)
                             (str (entity/id entity))
                             script
                             values))


(defn entity-indexed?
  ([es entity]
   ^{:doc "Determines whether or not an iRODS entity has been indexed.

           Parameters:
             es     - the elasticsearch connection
             entity - the entity being checked

           Throws:
             This function can throw an exception if it can't connect to elasticsearch."}
   (es-doc/present? es index (mapping-type-of entity) (str (entity/id entity))))

  ([es entity-type entity-id]
   ^{:doc "Determines whether or not an iRODS entity has been indexed.

           Parameters:
             es          - the elasticsearch connection
             entity-type - :collection|:data-object
             entity-id   - the UUID of the entity being checked

           Throws:
             This function can throw an exception if it can't connect to elasticsearch."}
   (es-doc/present? es index (mapping-type-of entity-type) (str entity-id))))


(defn index-collection
  "Indexes a collection.

   Parameters:
     es    - the elasticsearch connection
     coll  - the collection entity to index

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the collection is not in the iRODS data store."
  [es coll]
  (let [folder (prep/format-folder (entity/id coll)
                                   (entity/path coll)
                                   (entity/acl coll)
                                   (entity/creator coll)
                                   (entity/creation-time coll)
                                   (entity/modification-time coll)
                                   (entity/metadata coll))]
    (index-doc es collection-type folder)))


(defn index-data-object
  "Indexes a data object.

   Parameters:
     es        - the elasticsearch connection
     obj       - The CollectionAndDataObjectListingEntry of the data object to index
     creator   - (Optional) The username of the creator of the data object
     file-size - (Optional) The byte size of the data object
     file-type - (Optional) The media type of the data object

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the data object is not in the iRODS data store."
  [es obj & {:keys [creator file-size file-type]}]
  (let [file (prep/format-file (entity/id obj)
                               (entity/path obj)
                               (entity/acl obj)
                               (or creator (entity/creator obj))
                               (entity/creation-time obj)
                               (entity/modification-time obj)
                               (entity/metadata obj)
                               (or file-size (entity/size obj))
                               (or file-type (entity/media-type obj)))]
    (index-doc es data-object-type file)))


(defn remove-entity
  "Removes an iRODS entity from the search index.

   Parameters:
     es          - the elasticsearch connection
     entity-type - :collection|:data-object
     entity-id   - The UUID of the entity to remove

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [es entity-type entity-id]
  (when (entity-indexed? es entity-type entity-id)
    (es-doc/delete es index (mapping-type-of entity-type) (str entity-id))))


(defn remove-entities-like
  "Removes iRODS entities from the search index that have a path matching the provide glob. The glob
   supports * and ? wildcards with their typical meanings.

   Parameters:
     es        - the elasticsearch connection
     path-glob - the glob describing the paths of the entities to remove

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [es path-glob]
  (es-doc/delete-by-query-across-all-types es index (es-query/wildcard :path path-glob)))


; XXX - I wish I could think of a way to cleanly and simply separate out the document update logic
; from the update scripts calls in the following functions. It really belongs with the rest of the
; document logic in the doc-prep namespace.


(defn update-path
  "Updates the path of an entity and optionally its modification time

   Parameters:
     es       - the elasticsearch connection
     entity   - the entity to update
     path     - the entity's new path
     mod-time - (Optional) the entity's modification time"
  ([es entity path]
   (update-doc es
               entity
               "ctx._source.path = path;
                ctx._source.label = label;"
               {:path  path
                :label (file/basename path)}))

  ([es entity path mod-time]
    (update-doc es
                entity
                "ctx._source.path = path;
                 ctx._source.label = label;
                 ctx._source.dateModified = dateModified;"
                {:path         path
                 :label        (file/basename path)
                 :dateModified (prep/format-time mod-time)})))


(defn update-acl
  "Updates the indexed ACL of an entity.

   Parameters:
     es     - the elasticsearch connection
     entity - the entity whose ACL needs to be updated in elasticsearch

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [es entity]
  (update-doc es
              entity
              "ctx._source.userPermissions = permissions"
              {:permissions (prep/format-acl (entity/acl entity))}))


(defn update-metadata
  "Updates the indexed AVU metadata of an entity.

   Parameters:
     es     - the elasticsearch connection
     entity - The entity whose metadata needs to be updated in elasticsearch

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [es entity]
  (update-doc es
              entity
              "ctx._source.metadata = metadata"
              {:metadata (prep/format-metadata (entity/metadata entity))}))


(defn update-collection-modify-time
  "Updates the indexed modify time of a collection.

   Parameters:
     es   - the elasticsearch connection
     coll - the collection that was modified

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the collection has no index entry or is not in the iRODS data store."
  [es coll]
  (update-doc es
              coll
              "ctx._source.dateModified = dateModified"
              {:dateModified (prep/format-time (entity/modification-time coll))}))


(defn update-data-object
  "Updates the indexed data object. It will update the modification time, file size and optionally
   file type for the data object.

   Parameters:
     es        - the elasticsearch connection
     obj       - the data object that was modified
     file-size - The data object's file size in bytes.
     file-type - (OPTIONAL) The media type of the data object.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the data object has no index entry or is not in the iRODS data store."
  ([es obj file-size]
   (update-doc es
               obj
               "ctx._source.dateModified = dateModified;
                ctx._source.fileSize = fileSize;"
               {:dateModified (prep/format-time (entity/modification-time obj))
                :fileSize     file-size}))

  ([es obj file-size file-type]
   (update-doc es
               obj
               "ctx._source.dateModified = dateModified;
                ctx._source.fileSize = fileSize;
                ctx._source.fileType = fileType;"
               {:dateModified (prep/format-time (entity/modification-time obj))
                :fileSize     file-size
                :fileType     file-type})))
