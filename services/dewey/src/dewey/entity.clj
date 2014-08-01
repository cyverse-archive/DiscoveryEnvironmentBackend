(ns dewey.entity
  "This provides a uniform interface for accessing iRODS files and folders."
  (:require [clojure.tools.logging :as log]
            [clj-jargon.item-info :as info]
            [clj-jargon.lazy-listings :as lazy]
            [clj-jargon.metadata :as metadata])
  (:import [java.util UUID]
           [org.irods.jargon.core.exception FileNotFoundException]
           [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(defn- lookup-base
  [irods path]
  (try
    (.getCollectionAndDataObjectListingEntryAtGivenAbsolutePath (:lister irods) path)
    (catch FileNotFoundException _)))


(defn- mk-entity
  ([irods base]
   (when base
     (let [path (.getFormattedAbsolutePath base)
           id   (-> (metadata/get-attribute irods path "ipc_UUID") first :value)]
       (if id
         (mk-entity irods base id)
         (log/info path "doesn't appear to have a UUID. It may have just been renamed.")))))
  ([irods base id]
   {:irods irods
    :id    id
    :base  base}))


(defn lookup-entity
  ([irods path]
   ^{:doc "Retrieves an entity from iRODS.

           Parameters:
             irods - An open Jargon context
             path  - The absolute path to the entity

           Returns:
             It returns a representation of the entity suitable for use in this namespace. It
             returns nil if the entity doesn't exist."}
   (when path
     (mk-entity irods (lookup-base irods path))))

  ([irods type id]
   ^{:doc "Retrieves an entity from iRODS.

           Parameters:
             irods - An open Jargon context
             type  - :collection|:data-object
             id    - The UUID of the entity to lookup

           Returns:
             It returns a representation of the entity suitable for use in this namespace. It
             returns nil if the entity doesn't exist."}
   (when-let [path (case type
                     :collection  (first (metadata/list-collections-with-attr-value irods
                                                                                    "ipc_UUID"
                                                                                    id))
                     :data-object (first (metadata/list-files-with-avu irods "ipc_UUID" := id)))]
     (mk-entity irods (lookup-base irods path) id))))


(defn id
  "Retrieves the UUID of the entity.

   Parameter:
     entity - the entity of interest

   Returns:
     The entity's UUID."
  [entity]
  (:id entity))


(defn entity-type
  "Retrieves the type of an entity

   Parameter:
     entity - the entity of interest

   Returns:
     :collection for a collection and :data-object for a data object."
  [entity]
  (when-let [base (:base entity)]
    (cond
      (.isCollection base) :collection
      (.isDataObject base) :data-object)))


(defn path
  "Retrieves the absolute path of the entity.

   Parameter:
     entity - the entity of interest

   Returns:
     The entity's absolute path."
  [entity]
  (when entity (.getFormattedAbsolutePath (:base entity))))


(defn zone
  "Retrieves the zone where the entity is stored.

   Parameter:
     entity - the entity of interest

   Returns:
     The zone where the entity is stored."
  [entity]
  (:zone (:irods entity)))


(defn acl
  "Retrieves the ACL of the entity.

   Parameter:
     entity - the entity of interest

   Returns:
     The entity's ACL as a list of UserFilePermission objects."
  [entity]
  (case (entity-type entity)
    :collection  (.listPermissionsForCollection (:collectionAO (:irods entity)) (path entity))
    :data-object (.listPermissionsForDataObject (:dataObjectAO (:irods entity)) (path entity))))


(defn creator
  "Retrieves the username of the creator of then entity.

    Parameter:
      entity - the entity of interest

    Returns:
      It returns a map of the form {:name <username> :zone <authentication zone>} hold the creator's
      username and authentication zone."
  [entity]
  (when-let [base (:base entity)]
    {:name (.getOwnerName base) :zone (.getOwnerZone base)}))


(defn creation-time
  "Retrieves the time when the entity was created in iRODS.

   Parameter:
     entity - the entity of interest

   Returns:
     The Date object containing the creation time."
  [entity]
  (when entity (.getCreatedAt (:base entity))))


(defn modification-time
  "Retrieves the time when the entity was last modified.

   Parameter:
     entity - the entity of interest

   Returns:
     The Date object containing the modification time."
  [entity]
  (when entity (.getModifiedAt (:base entity))))


(defn size
  "For data objects, it retrieves the size in bytes of the physical file.  For collections, it
   returns 0.

   Parameter:
     entity - the entity of interest

   Returns:
     It returns the size in bytes for data objects and 0 for collections."
  [entity]
  (when-let [base (:base entity)]
    (if (.isDataObject base)
      (.getDataSize base)
      0)))


(defn media-type
  "Form data objects, this function returns the media type, if known.  For collections, it returns
   nil.

   Parameter:
     entity - the entity of interest

   Returns:
     It returns the media type, if known."
  [entity]
  (when (and entity (.isDataObject (:base entity)))
    (.getDataTypeName (info/data-object (:irods entity) (path entity)))))


(defn metadata
  "Retrieves the user metadata attached to a given entity.

   Parameter:
     entity - the entity of interest

   Returns:
     It returns a list of AVU maps.  An AVU map has the form

     {:attr  <attribute name>
      :value <attribute value>
      :unit  <attribute value's unit>}"
  [entity]
  (when entity
    (remove #(= "ipc_UUID" (:attr %)) (metadata/get-metadata (:irods entity) (path entity)))))


(defn parent
  "Retrieves the parent entity of an entity.

   Parameters:
     child - The child of the parent ot retrieve

   Returns:
     It returns the parent entity or nil if the child is the root entity."
  [child]
  (when (and child (not= "/" (path child)))
    (lookup-entity (:irods child) :collection (.getParentPath (:base child)))))


(defn child-collections
  "Retries all of the member collections of a given collection.

   Parameters:
     parent - The parent of the collections to retrieve.

   Returns:
     It returns a lazy sequence of the collection entities."
  [parent]
  (when-let [irods (:irods parent)]
    (map (partial mk-entity irods) (lazy/list-subdirs-in irods (path parent)))))


(defn child-data-objects
  "Retries all of the member data objects of a given collection.

   Parameters:
     parent - The parent of the data objects to retrieve.

   Returns:
     It returns a lazy sequence of the data object entities."
  [parent]
  (when-let [irods (:irods parent)]
    (map (partial mk-entity irods) (lazy/list-files-in irods (path parent)))))


(defn child-data-objects-like
  "Retrieves all of the data objects that belong to a collection that have names matching a given
   pattern.

   Parameters:
     parent    - The collection entity to inspect
     name-glob - The glob pattern each data object will match

   Returns:
     It returns a lazy sequence of data objects entity"
  [parent name-glob]
  (when-let [irods (:irods parent)]
    (->> (lazy/list-files-in irods (path parent))
      (filter #(re-matches name-glob (.getNodeLabelDisplayValue %)))
      (map (partial mk-entity irods)))))
