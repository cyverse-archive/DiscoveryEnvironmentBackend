(ns clj-jargon.listings
  (:use [clj-jargon.validations]
        [clj-jargon.gen-query]
        [clj-jargon.permissions]
        [clj-jargon.users :only [user-groups username->id]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.string :as string])
  (:import [org.irods.jargon.core.query RodsGenQueryEnum]))

(defn list-subdirs-rs
  [cm user coll-path]
  (execute-gen-query cm
   "select %s, %s, %s, %s where %s = '%s' and %s = '%s'"
   [RodsGenQueryEnum/COL_COLL_NAME
    RodsGenQueryEnum/COL_COLL_CREATE_TIME
    RodsGenQueryEnum/COL_COLL_MODIFY_TIME
    RodsGenQueryEnum/COL_COLL_ACCESS_TYPE
    RodsGenQueryEnum/COL_COLL_PARENT_NAME
    coll-path
    RodsGenQueryEnum/COL_COLL_ACCESS_USER_NAME
    user]))

(defn format-listing
  [format-fn perm-pos listing]
  (letfn [(select-listing [[_ v]]
            [(apply max-key #(perm-order-map (str->perm-const (nth % perm-pos))) v)])]
    (->> (apply concat listing)
         (map result-row->vec)
         (group-by first)
         (mapcat select-listing)
         (map format-fn))))

(defn- format-dir
  [[path create-time mod-time perms]]
  {:date-created  (str (* (Integer/parseInt create-time) 1000))
   :date-modified (str (* (Integer/parseInt mod-time) 1000))
   :file-size     0
   :hasSubDirs    true
   :path          path
   :label         (ft/basename path)
   :permission    (fmt-perm perms)})

(defn- list-subdirs
  [cm user coll-path]
  (sort-by (comp string/upper-case :label)
           (format-listing
            format-dir 3
            (map #(list-subdirs-rs cm % coll-path)
                 (conj (user-groups cm user) user)))))

(defn list-files-in-dir-rs
  [cm user-id coll-path]
  (execute-gen-query cm
   "select %s, %s, %s, %s, %s where %s = '%s' and %s = '%s'"
   [RodsGenQueryEnum/COL_DATA_NAME
    RodsGenQueryEnum/COL_D_CREATE_TIME
    RodsGenQueryEnum/COL_D_MODIFY_TIME
    RodsGenQueryEnum/COL_DATA_SIZE
    RodsGenQueryEnum/COL_DATA_ACCESS_TYPE
    RodsGenQueryEnum/COL_COLL_NAME
    coll-path
    RodsGenQueryEnum/COL_DATA_ACCESS_USER_ID
    user-id]))

(defn- format-file
  [coll-path [name create-time mod-time size perms]]
  {:date-created  (str (* (Integer/parseInt create-time) 1000))
   :date-modified (str (* (Integer/parseInt mod-time) 1000))
   :file-size     size
   :permissions   (perm-map-for perms)
   :id            (ft/path-join coll-path name)
   :label         name})

(defn- list-files-in-dir
  [cm user coll-path]
  (sort-by (comp string/upper-case :label)
           (format-listing
            (partial format-file coll-path) 4
            (map #(list-files-in-dir-rs cm (username->id cm %) coll-path)
                 (conj (user-groups cm user) user)))))

(defn list-dir-rs
  [cm user coll-path]
  (execute-gen-query cm
   "select %s, %s, %s, %s where %s = '%s' and %s = '%s'"
   [RodsGenQueryEnum/COL_COLL_NAME
    RodsGenQueryEnum/COL_COLL_CREATE_TIME
    RodsGenQueryEnum/COL_COLL_MODIFY_TIME
    RodsGenQueryEnum/COL_COLL_ACCESS_TYPE
    RodsGenQueryEnum/COL_COLL_NAME
    coll-path
    RodsGenQueryEnum/COL_COLL_ACCESS_USER_NAME
    user]))

(defn- dir-list-sort
  [one two]
  (> (Integer/parseInt (last one))
     (Integer/parseInt (last two))))

;; TODO: remove? obsolete?
(defn list-dir
  [cm user coll-path & {:keys [include-files include-subdirs]
                        :or   {include-files   false
                               include-subdirs true}}]
  (let [coll-path (ft/rm-last-slash coll-path)
        vec-res   (sort dir-list-sort (mapv result-row->vec (list-dir-rs cm user coll-path)))
        results   (map format-dir vec-res)
        listing   (first results)]
    (when-not (nil? listing)
      (reduce (fn [listing [_ k f]] (assoc listing k (f cm user coll-path)))
              listing
              (filter first
                      [[include-subdirs :folders list-subdirs]
                       [include-files :files list-files-in-dir]])))))

(defn last-dir-in-path
  [cm path]
  "Returns the name of the last directory in 'path'.

    Please note that this function works by calling
    getCollectionLastPathComponent on a Collection instance and therefore
    hits iRODS every time you call it. Don't call this from within a loop.

    Parameters:
      cm - The iRODS context map
      path - String containing the path for an item in iRODS.

    Returns:
      String containing the name of the last directory in the path."
  (validate-path-lengths path)
  (.getCollectionLastPathComponent
    (.findByAbsolutePath (:collectionAO cm) (ft/rm-last-slash path))))

(defn sub-collections
  [cm path]
  "Returns a sequence of Collections that reside directly in the directory
    refered to by 'path'.

    Parameters:
      cm - The iRODS context map
      path - String containing the path to a directory in iRODS.

    Returns:
      Sequence containing Collections (the Jargon kind) representing
      directories that reside under the directory represented by 'path'."
  (validate-path-lengths path)
  (.listCollectionsUnderPath (:lister cm) (ft/rm-last-slash path) 0))

(defn sub-collection-paths
  [cm path]
  "Returns a sequence of string containing the paths for directories
    that live under 'path' in iRODS.

    Parameters:
      cm - The iRODS context map
      path - String containing the path to a directory in iRODS.

    Returns:
      Sequence containing the paths for directories that live under 'path'."
  (validate-path-lengths path)
  (map
    #(.getFormattedAbsolutePath %)
    (sub-collections cm path)))

(defn sub-dir-maps
  [cm user list-obj filter-files]
  (let [abs-path (.getFormattedAbsolutePath list-obj)
        basename (ft/basename abs-path)
        lister   (:lister cm)]
    {:id            abs-path
     :label         (ft/basename abs-path)
     :permissions   (collection-perm-map cm user abs-path)
     :hasSubDirs    (pos? (count (.listCollectionsUnderPath lister abs-path 0)))
     :date-created  (str (long (.. list-obj getCreatedAt getTime)))
     :date-modified (str (long (.. list-obj getModifiedAt getTime)))}))

(defn sub-file-maps
  [cm user list-obj]
  (let [abs-path    (.getFormattedAbsolutePath list-obj)]
    {:id            abs-path
     :label         (ft/basename abs-path)
     :permissions   (dataobject-perm-map cm user abs-path)
     :date-created  (str (long (.. list-obj getCreatedAt getTime)))
     :date-modified (str (long (.. list-obj getModifiedAt getTime)))
     :file-size     (.getDataSize list-obj)}))

(defn list-all
  [cm dir-path]
  (validate-path-lengths dir-path)
  (.listDataObjectsAndCollectionsUnderPath (:lister cm) dir-path))
