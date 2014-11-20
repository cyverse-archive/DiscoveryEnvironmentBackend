(ns clj-icat-direct.icat
  (:use [clojure.java.io :only [file]])
  (:require [clojure.string :as string]
            [korma.db :as db]
            [korma.core :as k]
            [clj-icat-direct.queries :as q])
  (:import [clojure.lang ISeq Keyword]))


(defn icat-db-spec
  "Creates a Korma db spec for the ICAT."
  [hostname user pass & {:keys [port db]
                         :or {port 5432
                              db "ICAT"}}]
  (db/postgres {:host     hostname
                :port     port
                :db       db
                :user     user
                :password pass}))

(defn setup-icat
  "Defines the icat database. Pass in the return value of icat-db-spec."
  [icat-db-spec]
  (db/defdb icat icat-db-spec))

(defn- run-simple-query
  "Runs one of the defined queries against the ICAT. It's considered a simple query if it doesn't
   require string formatting."
  [query-kw & args]
  (if-not (contains? q/queries query-kw)
    (throw (Exception. (str "query " query-kw " is not defined."))))

  (k/exec-raw icat [(get q/queries query-kw) args] :results))

(defn- run-query-string
  "Runs the passed in query string. Doesn't check to see if it's defined in
   clj-icat-direct.queries first."
  [query & args]
  (k/exec-raw icat [query args] :results))

(defn number-of-files-in-folder
  "Returns the number of files in a folder that the user has access to."
  [user zone folder-path]
  (-> (run-simple-query :count-files-in-folder user zone folder-path) first :count))

(defn number-of-folders-in-folder
  "Returns the number of folders in the specified folder that the user has access to."
  [user zone folder-path]
  (-> (run-simple-query :count-folders-in-folder user zone folder-path) first :count))


(defn ^Integer number-of-items-in-folder
  "Returns the total number of files and folders in the specified folder that the user has access
   to and where the files have the given info types.

   Parameters:
     user        - the username of the user
     zone        - the user's authentication zone
     folder-path - the absolute path to the folder being inspected
     entity-type - the type of entities to return (:any|:file|:folder), :any means both files and
                   folders
     info-types  - the info-types of the files to count, if empty, all files are counted

   Returns:
     It returns the total number of folders combined with the total number of files with the given
     info types."
  [^String user ^String zone ^String folder-path ^Keyword entity-type & [info-types]]
  (let [type-cond (q/mk-file-type-cond info-types)
        query     (case entity-type
                    :any    (q/mk-count-items-in-folder user zone folder-path type-cond)
                    :file   (q/mk-count-files-in-folder user zone folder-path type-cond)
                    :folder (q/mk-count-folders-in-folder user zone folder-path)
                            (throw (Exception. (str "invalid entity type " entity-type))))]
    (-> (run-query-string query) first :total)))


(defn number-of-all-items-under-folder
  "Returns the total number of files and folders in the specified folder and all
   sub-folders that the user has access to."
  [user zone folder-path]
  (-> (run-simple-query :count-all-items-under-folder user zone folder-path folder-path)
      (first)
      (:total)))


(defn ^Integer number-of-bad-items-in-folder
  "Returns the total number of files and folders in the specified folder that the user has access to
   and where the files have the given info types, but should be marked as having a bad name in the
   client.

   Parameters:
     user        - the username of the authorized user
     zone        - the user's authentication zone
     folder-path - The absolute path to the folder of interest
     entity-type - the type of entities to return (:any|:file|:folder), :any means both files and
                   folders
     info-types  - the info-types of the files to count, if empty, all files are considered
     bad-chars   - If a name contains one or more of these characters, the item will be marked as
                   bad
     bad-names   - This is a sequence of names that are bad
     bad-paths   - This is an array of paths to items that will be marked as badr.

   Returns:
     It returns the total."
  [& {:keys [user zone folder-path entity-type info-types bad-chars bad-names bad-paths]}]
  (let [info-type-cond  (q/mk-file-type-cond info-types)
        bad-file-cond   (q/mk-bad-file-cond folder-path bad-chars bad-names bad-paths)
        bad-folder-cond (q/mk-bad-folder-cond folder-path bad-chars bad-names bad-paths)
        query-ctor      (case entity-type
                          :any    q/mk-count-bad-items-in-folder
                          :file   q/mk-count-bad-files-in-folder
                          :folder q/mk-count-bad-folders-in-folder
                                  (throw (Exception. (str "invalid entity type " entity-type))))
        query           (query-ctor
                          :user            user
                          :zone            zone
                          :parent-path     folder-path
                          :info-type-cond  info-type-cond
                          :bad-file-cond   bad-file-cond
                          :bad-folder-cond bad-folder-cond)]
    (-> (run-query-string query) first :total)))


(defn folder-permissions-for-user
  "Returns the highest permission value for the specified user on the folder."
  [user folder-path]
  (let [sorter (partial sort-by :access_type_id)]
    (-> (run-simple-query :folder-permissions-for-user user folder-path)
      sorter last :access_type_id)))

(defn file-permissions-for-user
  "Returns the highest permission value for the specified user on the file."
  [user file-path]
  (let [sorter   (partial sort-by :access_type_id)
        dirname  #(.getParent (file %))
        basename #(.getName (file %))]
    (-> (run-simple-query :file-permissions-for-user user (dirname file-path) (basename file-path))
      sorter last :access_type_id)))

(defn- add-permission
  [user {:keys [full_path type] :as item-map} ]
  (let [perm-func (if (= type "dataobject") file-permissions-for-user folder-permissions-for-user)]
    (assoc item-map :access_type_id (perm-func user full_path))))

(defn list-folders-in-folder
  "Returns a listing of the folders contained in the specified folder that the user has access to."
  [user zone folder-path]
  (map (partial add-permission user)
       (run-simple-query :list-folders-in-folder user zone folder-path)))


(defn ^ISeq folder-path-listing
  "Returns a complete folder listing for everything visible to a given user.

   Parameters:
     user        - the name of the user
     zone        - the authentication zone of the user
     folder-path - the absolute path to the folder

   Returns:
     It returns a sequence of paths."
  [^String user ^String zone ^String folder-path]
  (map :full_path (run-simple-query :folder-listing user zone folder-path)))


(defn- fmt-info-type
  [record]
  (if (and (= "dataobject" (:type record))
           (empty? (:info_type record)))
    (assoc record :info_type "raw")
    record))


(defn- resolve-sort-column
  [col-key]
  (if-let [col (get q/sort-columns col-key)]
    col
    (throw (Exception. (str "invalid sort column " col-key)))))


(defn- resolve-sort-direction
  [direction-key]
  (if-let [direction (get q/sort-directions direction-key)]
    direction
    (throw (Exception. (str "invalid sort direction" direction-key)))))


(defn ^ISeq paged-folder-listing
  "Returns a page from a folder listing.

   Parameters:
     user           - the name of the user determining access privileges
     zone           - the authentication zone of the user
     folder-path    - the folder to list the contents of
     entity-type    - the type of entities to return (:any|:file|:folder), :any means both files and
                      folders
     sort-column    - the column to sort by
                      (:type|:modify-ts|:create-ts|:data-size|:base-name|:full-path)
     sort-direction - the sorting direction (:asc|:desc)
     limit          - the maximum number of results to return
     offset         - the number of results to skip after sorting and before returning results
     file-types     - the info types of interest

   Returns:
     It returns a page of results.

   Throws:
     It throws an exception if a validation fails."
  [& {:keys [user zone folder-path entity-type sort-column sort-direction limit offset info-types]}]
  (let [query-ctor (case entity-type
                     :any    q/mk-paged-folder
                     :file   q/mk-paged-files-in-folder
                     :folder q/mk-paged-folders-in-folder
                             (throw (Exception. (str "invalid entity type " entity-type))))
        query (query-ctor
                :user           user
                :zone           zone
                :parent-path    folder-path
                :info-type-cond (q/mk-file-type-cond info-types)
                :sort-column    (resolve-sort-column sort-column)
                :sort-direction (resolve-sort-direction sort-direction))]
    (map fmt-info-type (run-query-string query limit offset))))


(defn select-files-with-uuids
  "Given a set of UUIDs, it returns a list of UUID-path pairs for each UUID that corresponds to a
   file."
  [uuids]
  ; This can't be run as a simple query.  I suspect the UUID db type is causing trouble
  (let [query (format (:select-files-with-uuids q/queries) (q/prepare-text-set uuids))]
    (run-query-string query)))

(defn select-folders-with-uuids
  "Given a set of UUIDs, it returns a list of UUID-path pairs for each UUID that corresponds to a
   folder."
  [uuids]
  ; This can't be run as a simple query. I suspect the UUID db type is causing trouble
  (let [query (format (:select-folders-with-uuids q/queries) (q/prepare-text-set uuids))]
    (run-query-string query)))

(defn ^ISeq paged-uuid-listing
  "Returns a page of filesystem entries corresponding to a list a set of UUIDs.

   Parameters:
     user        - the name of the user determining access privileges
     zone        - the authentication zone of the user
     sort-column - the column to sort by (type|modify-ts|create-ts|data-size|base-name|full-path)
     sort-order  - the sorting direction (asc|desc)
     limit       - the maximum number of results to return
     offset      - the number of results to skip after sorting and before returning results
     uuids       - the list of UUIDS to look up.
     file-types  - the info types of interest

   Returns:
     The result set"
  [^String  user
   ^String  zone
   ^Keyword sort-column
   ^Keyword sort-order
   ^Long    limit
   ^Long    offset
   ^ISeq    uuids
   ^ISeq    file-types]
  (if (empty? uuids)
    []
    (let [uuid-set (q/prepare-text-set uuids)
          ft-cond  (q/mk-file-type-cond file-types)
          sc       (resolve-sort-column sort-column)
          so       (resolve-sort-direction sort-order)
          query    (format (:paged-uuid-listing q/queries) uuid-set ft-cond sc so)]
      (map fmt-info-type (run-query-string query user zone limit offset)))))


(defn ^Long number-of-uuids-in-folder
  "Returns the number of entities that have provided UUIDs, are visible to the given user and are a
   folder or have one of the given file types.

   Parameters:
     user       - the name of the user determining visibility
     zone       - the authentication zone of the user
     uuids       - the list of UUIDS to look up.
     file-types  - the info types of interest

   Returns:
     The result set"
  [^String user ^String zone ^ISeq uuids ^ISeq file-types]
  (if (empty? uuids)
    0
    (let [uuid-set (q/prepare-text-set uuids)
          ft-cond  (q/mk-file-type-cond file-types)
          query    (format (q/mk-count-uuids-of-file-type) uuid-set ft-cond)]
      (:total (first (run-query-string query user zone))))))
