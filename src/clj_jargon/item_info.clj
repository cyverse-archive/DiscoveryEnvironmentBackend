(ns clj-jargon.item-info
  (:use [clj-jargon.validations])
  (:require [clojure-commons.file-utils :as ft])
  (:import [org.irods.jargon.core.pub.domain ObjStat$SpecColType]
           [org.irods.jargon.core.query CollectionAndDataObjectListingEntry$ObjectType]))

(def collection-type CollectionAndDataObjectListingEntry$ObjectType/COLLECTION)
(def dataobject-type CollectionAndDataObjectListingEntry$ObjectType/DATA_OBJECT)

(defn trash-base-dir
  "Returns the base trash directory either for all users or for a specified user."
  ([cm]
     (ft/path-join "/" (:zone cm) "trash" "home" (:username cm)))
  ([cm user]
     (ft/path-join (trash-base-dir cm) user)))

(defn file
  [cm path]
  "Returns an instance of IRODSFile representing 'path'. Note that path
    can point to either a file or a directory.

    Parameters:
      path - String containing a path.

    Returns: An instance of IRODSFile representing 'path'."
  (validate-path-lengths path)
  (.instanceIRODSFile (:fileFactory cm) path))

(defn exists?
  [cm path]
  "Returns true if 'path' exists in iRODS and false otherwise.

    Parameters:
      path - String containing a path.

    Returns: true if the path exists in iRODS and false otherwise."
  (validate-path-lengths path)
  (.exists (file cm path)))

(defn paths-exist?
  [cm paths]
  "Returns true if the paths exist in iRODS.

    Parameters:
      paths - A sequence of strings containing paths.

    Returns: Boolean"
  (doseq [p paths] (validate-path-lengths p))
  (zero? (count (filter #(not (exists? cm %)) paths))))

(defn jargon-type-check
  [cm check-type path]
  (= check-type (.getObjectType (.getObjStat (:fileSystemAO cm) path))))

(defn is-file?
  [cm path]
  "Returns true if the path is a file in iRODS, false otherwise."
  (validate-path-lengths path)
  (jargon-type-check cm dataobject-type path))

(defn is-dir?
  [cm path]
  "Returns true if the path is a directory in iRODS, false otherwise."
  (validate-path-lengths path)
  (jargon-type-check cm collection-type path))

(defn is-linked-dir?
  [cm path]
  "Indicates whether or not a directory (collection) is actually a link to a
   directory (linked collection).

   Parameters:
     cm - the context map
     path - the absolute path to the directory to check.

   Returns:
     It returns true if the path points to a linked directory, otherwise it
     returns false."
  (validate-path-lengths path)
  (= ObjStat$SpecColType/LINKED_COLL
     (.. (:fileFactory cm)
       (instanceIRODSFile (ft/rm-last-slash path))
       initializeObjStatForFile
       getSpecColType)))

(defn data-object
  [cm path]
  "Returns an instance of DataObject represeting 'path'."
  (validate-path-lengths path)
  (.findByAbsolutePath (:dataObjectAO cm) path))

(defn collection
  [cm path]
  "Returns an instance of Collection (the Jargon version) representing
    a directory in iRODS."
  (validate-path-lengths path)
  (.findByAbsolutePath (:collectionAO cm) (ft/rm-last-slash path)))

(defn lastmod-date
  [cm path]
  "Returns the date that the file/directory was last modified."
  (validate-path-lengths path)
  (cond
    (is-dir? cm path)  (str (long (.getTime (.getModifiedAt (collection cm path)))))
    (is-file? cm path) (str (long (.getTime (.getUpdatedAt (data-object cm path)))))
    :else              nil))

(defn created-date
  [cm path]
  "Returns the date that the file/directory was created."
  (validate-path-lengths path)
  (cond
    (is-dir? cm path)  (str (long (.. (collection cm path) getCreatedAt getTime)))
    (is-file? cm path) (str (long (.. (data-object cm path) getCreatedAt getTime)))
    :else              nil))

(defn- dir-stat
  [cm path]
  "Returns status information for a directory."
  (validate-path-lengths path)
  (let [coll (collection cm path)]
    {:id            path
     :path          path
     :type          :dir
     :date-created  (long (.. coll getCreatedAt getTime))
     :date-modified (long (.. coll getModifiedAt getTime))}))

(defn- file-stat
  [cm path]
  "Returns status information for a file."
  (validate-path-lengths path)
  (let [data-obj (data-object cm path)]
    {:id            path
     :path          path
     :type          :file
     :file-size     (.getDataSize data-obj)
     :date-created  (long (.. data-obj getUpdatedAt getTime))
     :date-modified (long (.. data-obj getUpdatedAt getTime))}))

(defn stat
  [cm path]
  "Returns status information for a path."
  (validate-path-lengths path)
  (cond
   (is-dir? cm path)  (dir-stat cm path)
   (is-file? cm path) (file-stat cm path)
   :else              nil))

(defn file-size
  [cm path]
  "Returns the size of the file in bytes."
  (validate-path-lengths path)
  (.getDataSize (data-object cm path)))

(defn quota-map
  [quota-entry]
  (hash-map
    :resource (.getResourceName quota-entry)
    :zone     (.getZoneName quota-entry)
    :user     (.getUserName quota-entry)
    :updated  (str (.getTime (.getUpdatedAt quota-entry)))
    :limit    (str (.getQuotaLimit quota-entry))
    :over     (str (.getQuotaOver quota-entry))))

(defn quota
  [cm user]
  (mapv quota-map (.listQuotaForAUser (:quotaAO cm) user)))
