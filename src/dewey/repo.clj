(ns dewey.repo
  "The logic for interfacing with an iRODS data store."
  (:require [clj-jargon.init :as r-init]
            [clj-jargon.item-info :as r-info]
            [clj-jargon.lazy-listings :as r-lazy]
            [clj-jargon.metadata :as r-meta])
  (:import  [java.util Date]
            [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(defn- mk-user
  [name zone]
  {:name name :zone zone})


(defmulti ^{:private true} get-acl #(vector %2 (type %3)))

(defmethod get-acl [:collection String]
  [ctx _ path]
  (.listPermissionsForCollection (:collectionAO ctx) path))

(defmethod get-acl [:data-object String]
  [ctx _ path]
  (.listPermissionsForDataObject (:dataObjectAO ctx) path))

(defmethod get-acl [:collection CollectionAndDataObjectListingEntry]
  [ctx _ entry]
  (.getUserFilePermission entry))

(defmethod get-acl [:data-object CollectionAndDataObjectListingEntry]
  [ctx _ entry]
  (.getUserFilePermission entry))


(defmulti ^{:private true} get-creator #(vector %2 (type %3)))

(defmethod get-creator [:collection String]
  [ctx _ path]
  (let [coll (r-info/collection ctx path)]
    (mk-user (.getCollectionOwnerName coll) (.getCollectionOwnerZone coll))))

(defmethod get-creator [:data-object String]
  [ctx _ path]
  (let [obj (r-info/data-object ctx path)]
    (mk-user (.getDataOwnerName obj) (.getDataOwnerZone obj))))

(defmethod get-creator [:collection CollectionAndDataObjectListingEntry]
  [ctx _ entry]
  (mk-user (.getOwnerName entry) (.getOwnerZone entry)))

(defmethod get-creator [:data-object CollectionAndDataObjectListingEntry]
  [ctx _ entry]
  (mk-user (.getOwnerName entry) (.getOwnerZone entry)))


(defmulti ^{:private true} get-data-object-size #(type %2))

(defmethod get-data-object-size String
  [ctx path]
  (r-info/file-size ctx path))

(defmethod get-data-object-size CollectionAndDataObjectListingEntry
  [ctx obj]
  (.getDataSize obj))


(defmulti ^{:private true} get-data-object-type #(type %2))

(defmethod get-data-object-type String
  [ctx path]
  (.getDataTypeName (r-info/data-object ctx path)))

(defmethod get-data-object-type CollectionAndDataObjectListingEntry
  [ctx obj]
  (.getDataTypeName (r-info/data-object ctx (.getFormattedAbsolutePath obj))))


(defmulti ^{:private true} get-date-created #(type %2))

(defmethod get-date-created String
  [ctx path]
  (-> (r-info/created-date ctx path) Long/valueOf Date.))

(defmethod get-date-created CollectionAndDataObjectListingEntry
  [ctx entry]
  (.getCreatedAt entry))


(defmulti ^{:private true} get-date-modified #(type %2))

(defmethod get-date-modified String
  [ctx path]
  (-> (r-info/lastmod-date ctx path) Long/valueOf Date.))

(defmethod get-date-modified CollectionAndDataObjectListingEntry
  [ctx entry]
  (.getModifiedAt entry))


(defmulti ^{:private true} get-metadata #(type %2))

(defmethod get-metadata String
  [ctx path]
  (r-meta/get-metadata ctx path))

(defmethod get-metadata CollectionAndDataObjectListingEntry
  [ctx entity]
  (r-meta/get-metadata ctx (.getFormattedAbsolutePath entity)))


(defprotocol DataStore
  "Defines the interface for queries on the data store. These operations have been abstracted into
   an interface to facilitate unit testing w/o a live iRODS."

  (acl [_ entity-type entity]
    "Retrieves an ACL for a collection or data object.

     Parameters:
       entity-type - :collection|:data-object
       entity      - The path to the entity or a corresponding CollectionAndDataObjectListingEntry
                     object.

     Returns:
       It returns a list of UserFilePermission objects.

     Throws:
       This method can throw an exception if the connection to the data store is lost or the entity
       is not in the data store.")

  (creator [_ entity-type entity]
    "Retrieves the creator info for a collection or data object.

     Parameters:
       entity-type - :collection|:data-object
       entity      - The path to the entity or a corresponding CollectionAndDataObjectListingEntry
                     object.

     Returns:
       It returns a map of the following form.

       {:name name-str
        :zone zone-str}

     Throws:
       This method can throw an exception if the connection to the data store is lost or the entity
       is not in the data store.")

  (data-object-size [_ data-object]
    "Retrieves the size in bytes of a data object.

     Parameters:
       data-object - The path to the data object or a corresponding
                     CollectionAndDataObjectListingEntry object

     Returns:
       The size in bytes

     Throws:
       This method can throw an exception if the connection to the data store is lost or the data
       object is not in the data store.")

  (data-object-type [_ data-object]
    "Retrieves the media type of a data object.

     Parameters:
       data-object - The path to the data object or a corresponding
                     CollectionAndDataObjectListingEntry object

     Returns:
       The media type

     Throws:
       This method can throw an exception if the connection to the data store is lost or the data
       object is not in the data store.")

  (date-created [_ entity]
    "Retrieves the creation time for a collection or data object.

     Parameters:
       entity - The path to the entity or a corresponding CollectionAndDataObjectListingEntry object

     Returns:
       The creation time

     Throws:
       This method can throw an exception if the connection to the data store is lost or the entity
       is not in the data store.")

  (date-modified [_ entity]
    "Retrieves the time when collection or data object was last modified.

     Parameters:
       entity - The path to the entity or a corresponding CollectionAndDataObjectListingEntry object

     Returns:
       The time of last modification

     Throws:
       This method can throw an exception if the connection to the data store is lost or the entity
       is not in the data store.")

  (exists? [_ path]
    "Determines whether the given path points to a collection or data object.

     Parameters:
       path - the path to the possible collection or data object

     Returns:
       Returns true if the path points to something, otherwise false

     Throws:
       This method can throw an exception if the connection to the data store is lost.")

  (data-objects-in [_ path]
    "Retrieves a list of all the data objects in a given collection. It doesn't recurse into member
     collections.

     Parameters:
       path - the path to the parent collection

     Returns:
       It returns a list of CollectionAndDataObjectListingEntry objects, one for each member data
       object.

     Throws:
       This method can throw an exception if the connection to the data store is lost or the parent
       collection is not in the data store.")

  (collections-in [_ path]
    "Retrieves a list of all the member collections in a given collection. It doesn't recurse into
     member collections.

     Parameters:
       path - the path to the parent collection

     Returns:
       It returns a list of CollectionAndDataObjectListingEntry objects, one for each member
       collection.

     Throws:
       This method can throw an exception if the connection to the data store is lost or the parent
       collection is not in the data store.")

  (metadata [_ entity]
    "Retrieves the AVU metadata attached to a given collection or data object.

     Parameters:
       entity - The path to the entity or a corresponding CollectionAndDataObjectListingEntry object

     Returns:
       It returns a list of maps. Each map represents an AVU triple and has the following form.

       {:attr  name-str
        :value value-str
        :unit  unit-str}

     Throws:
       This method can throw an exception if the connection to the data store is lost or the entity
       is not in the data store.")

  (zone [_]
    "Retrieves the authentication zone.

     Returns:
       It returns the name of the authentication zone."))


(defrecord ^{:private true} IRODS [ctx]
  DataStore

  (acl [_ entity-type entity] (get-acl ctx entity-type entity))
  (creator [_ entity-type entity] (get-creator ctx entity-type entity))
  (data-object-size [_ obj] (get-data-object-size ctx obj))
  (data-object-type [_ obj] (get-data-object-type ctx obj))
  (date-created [_ entity] (get-date-created ctx entity))
  (date-modified [_ entity] (get-date-modified ctx entity))
  (exists? [_ path] (r-info/exists? ctx path))
  (data-objects-in [_ path] (r-lazy/list-files-in ctx path))
  (collections-in [_ path] (r-lazy/list-subdirs-in ctx path))
  (metadata [_ entity] (get-metadata ctx entity))
  (zone [_] (:zone ctx)))


(defn do-with-irods
  "This function performs an operation with an open iRODS connection. The connection is opened
   immediately before the operation and closed immediately afterward.

   Parameters:
     irods-cfg - An irods-cfg map for an initialized clj-jargon library.
     perform   - the operation to perform. It must accept a DataStore object as its only argument.

   Returns:
     It returns the result of the operation.

   Throws:
     It throws an exception if it fails to connect to iRODS. Also any exceptions that escape perform
     will be thrown as well."
  [irods-cfg perform]
  (r-init/with-jargon irods-cfg [ctx]
    (perform (->IRODS ctx))))