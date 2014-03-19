(ns dewey.indexing
  "This is the logic for making changes to search index."
  (:require [clojurewerkz.elastisch.query :as es-query]
            [clojurewerkz.elastisch.rest.document :as es-doc]
            [dewey.doc-prep :as prep])
  (:import [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(def ^{:private true} index "data")

(def ^{:private true} collection-type "folder")
(def ^{:private true} data-object-type "file")


(defmulti ^{:private true} format-id type)

(defmethod format-id String
  [path]
  path)

(defmethod format-id CollectionAndDataObjectListingEntry
  [entry]
  (.getFormattedAbsolutePath entry))


(defn- mapping-type-of
  [entity-type]
  (case entity-type
    :collection  collection-type
    :data-object data-object-type))


(defn entity-indexed?
  "Determines whether or not an iRODS entity has been indexed.

   Parameters:
     entity-type - :collection|:data-object
     entity      - The path to the entity or a CollectionAndDataObjectListingEntry object for the
                   entity.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [entity-type entity]
  (es-doc/present? index (mapping-type-of entity-type) (format-id entity)))


(defn- index-entry
  [mapping-type entry]
  (es-doc/create index mapping-type entry :id (:id entry)))


(defn index-collection
  "Indexes a collection. It will use the provided iRODS proxy to look up any needed information.

   Parameters:
     irods      - the connected iRODS proxy
     collection - the path to the collection or a CollectionAndDataObjectListingEntry object for
                  it.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the collection is not in the iRODS data store."
  [irods collection]
  (let [entry (prep/format-folder (format-id collection)
                                  (.acl irods :collection collection)
                                  (.creator irods :collection collection)
                                  (.date-created irods collection)
                                  (.date-modified irods collection)
                                  (.metadata irods collection))]
    (index-entry collection-type entry)))


(defn index-data-object
  "Indexes a data object. It will use the provided iRODS proxy to look up any needed information.

   Parameters:
     irods       - the connected iRODS proxy
     data-object - the path to the collection or a CollectionAndDataObjectListingEntry object for
                   it.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. The
     function can also throw one if the data object is not in the iRODS data store."
  [irods data-object & {:keys [creator file-size file-type]}]
  (let [entry (prep/format-file (format-id data-object)
                                (.acl irods :data-object data-object)
                                (or creator (.creator irods :data-object data-object))
                                (.date-created irods data-object)
                                (.date-modified irods data-object)
                                (.metadata irods data-object)
                                (or file-size (.data-object-size irods data-object))
                                (or file-type (.data-object-type irods data-object)))]
    (index-entry data-object-type entry)))


(defn remove-entity
  "Removes an iRODS entity from the search index.

   Parameters:
     entity-type - :collection|:data-object
     entity      - the iRODS path to the entity or a CollectionAndDataObjectListingEntry object for
                   it.

   Throws:
     This functino can throw an exception if it can't connect to elasticsearch."
  [entity-type entity]
  (when (entity-indexed? entity-type entity)
    (es-doc/delete index (mapping-type-of entity-type) (format-id entity))))


(defn remove-entities-like
  "Removes iRODS entities from the search index that have a path matching the provide glob. The glob
   supports * and ? wildcards with their typical meanings.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch."
  [path-glob]
  (es-doc/delete-by-query-across-all-types index (es-query/wildcard :path path-glob)))


; XXX - I wish I could think of a way to cleanly and simply separate out the document update logic
; from the update scripts calls in the following functions. It really belongs with the rest of the
; document logic in the doc-prep namespace.


(defn update-acl
  "Updates the indexed ACL of an entity. The ACL will be retrieve from the iRODS data store if
   needed.

   Parameters:
     irods       - the connected iRODS proxy
     entity-type - :collection|:data-object
     entity      - The iRODS path to the entity or a CollectionAndDataObjectListingEntry object for
                   it.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [irods entity-type entity]
  (es-doc/update-with-script index
    (mapping-type-of entity-type)
    (format-id entity)
    "ctx._source.userPermissions = permissions"
    {:permissions (prep/format-acl (.acl irods entity-type entity))}))


(defn update-metadata
  "Updates the indexed AVU metadata of an entity. The metadata will be retrieve from the iRODS data
   store.

   Parameters:
     irods       - the connected iRODS proxy
     entity-type - :collection|:data-object
     entity      - The iRODS path to the entity or a CollectionAndDataObjectListingEntry object for
                   it.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can also
     throw if the entity has no index entry or is not in the iRODS data store."
  [irods entity-type entity]
  (es-doc/update-with-script index
                             (mapping-type-of entity-type)
                             (format-id entity)
                             "ctx._source.metadata = metadata"
                             {:metadata (prep/format-metadata (.metadata irods entity))}))


(defn update-collection-modify-time
  "Updates the indexed modify time of a collection. The modify time will be retrieved from the iRODS
   data store if needed.

   Parameters:
     irods      - The connected iRODS proxy
     collection - The iRODS path to the collection or a CollectionAndDataObjectListingEntry object
                  for it.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the collection has no index entry or is not in the iRODS data store."
  [irods collection]
  (es-doc/update-with-script index
    collection-type
    (format-id collection)
    "ctx._source.dateModified = dateModified;"
    {:dateModified (prep/format-time (.date-modified irods collection))}))


(defn- update-obj-with-script
  [obj script vals]
  (es-doc/update-with-script index data-object-type (format-id obj) script vals))


(defn update-data-object
  "Updates the indexed data object. It will update the modification time, file size and optionally
   file type for the data object. The modify time will be retrieved from the iRODS data store if
   needed.

   Parameters:
     irods       - The connected iRODS proxy
     data-object - The iRODS path to the collection or a CollectionAndDataObjectListingEntry object
                   for it.
     file-size   - The data object's file size in bytes.
     file-type   - (OPTIONAL) The media type of the data object.

   Throws:
     This function can throw an exception if it can't connect to elasticsearch or iRODS. It can
     also throw if the data object has no index entry or is not in the iRODS data store."
  ([irods data-object file-size]
    (update-obj-with-script data-object
                            "ctx._source.dateModified = dateModified;
                             ctx._source.fileSize = fileSize;"
                            {:dateModified (prep/format-time (.date-modified irods data-object))
                             :fileSize     file-size}))
  ([irods data-object file-size file-type]
    (update-obj-with-script data-object
                            "ctx._source.dateModified = dateModified;
                             ctx._source.fileSize = fileSize;
                             ctx._source.fileType = fileType;"
                            {:dateModified (prep/format-time (.date-modified irods data-object))
                             :fileSize     file-size
                             :fileType     file-type})))
