(ns data-info.services.directory
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [slingshot.slingshot :refer [throw+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as item]
            [clj-jargon.permissions :as perm]
            [clj-jargon.validations :as jv]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.services.uuids :as uuids]
            [data-info.services.common-paths :as paths]
            [data-info.services.icat :as jargon]
            [data-info.services.validators :as validators])
  (:import [clojure.lang ISeq]))


(defn ^ISeq get-paths-in-folder
  "Returns all of the paths of the members of a given folder that are visible to a given user.

   Parameters:
     user   - the username of the user
     folder - the folder to inspect
     limit  - (OPTIONAL) if provided, only the first <limit> members will be returned.

   Returns:
     It returns a list of paths."
  ([^String user ^String folder]
   (icat/folder-path-listing user (cfg/irods-zone) folder))

  ([^String user ^String folder ^Integer limit]
   (let [listing (icat/paged-folder-listing user (cfg/irods-zone) folder :full-path :asc limit 0)]
     (map :full_path listing))))


(defn- filtered-paths
  "Returns a seq of full paths that should not be included in paged listing."
  [user]
  [(cfg/community-data)
   (ft/path-join (cfg/irods-home) user)
   (ft/path-join (cfg/irods-home) "public")])


(defn- should-filter?
  "Returns true if the map is okay to include in a directory listing."
  [user path-to-check]
  (let [fpaths (set (concat (cfg/filter-files) (filtered-paths user)))]
    (or (contains? fpaths path-to-check)
        (not (paths/valid-path? path-to-check)))))


(defn- page-entry->map
  "Turns a entry in a paged listing result into a map containing file/directory information that can
   be consumed by the front-end."
  [user {:keys [type full_path base_name data_size modify_ts create_ts access_type_id uuid]}]
  (let [base-map {:id            uuid
                  :path          full_path
                  :label         base_name
                  :filter        (or (should-filter? user full_path)
                                     (should-filter? user base_name))
                  :file-size     data_size
                  :date-created  (* (Integer/parseInt create_ts) 1000)
                  :date-modified (* (Integer/parseInt modify_ts) 1000)
                  :permission    (perm/fmt-perm access_type_id)}]
    (if (= type "dataobject")
      base-map
      (assoc base-map :hasSubDirs true :file-size 0))))


(defn- page->map
  "Transforms an entire page of results for a paged listing in a map that can be returned to the
   client."
  [user page]
  (let [entry-types (group-by :type page)
        do          (get entry-types "dataobject")
        collections (get entry-types "collection")
        xformer     (partial page-entry->map user)]
    {:files   (mapv xformer do)
     :folders (mapv xformer collections)}))


(defn- list-directories
  "Lists the directories contained under path."
  [user path]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (jargon/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-exists cm path)
      (validators/path-readable cm user path)
      (validators/path-is-dir cm path)
      (let [stat (item/stat cm path)
            zone (cfg/irods-zone)
            uuid (:uuid (uuids/uuid-for-path cm user path))]
        (merge
          (hash-map
            :id            uuid
            :path          path
            :label         (paths/id->label user path)
            :filter        (should-filter? user path)
            :permisssion   (perm/permission-for cm user path)
            :hasSubDirs    true
            :date-created  (:date-created stat)
            :date-modified (:date-modified stat)
            :file-size     0)
          (dissoc (page->map user (icat/list-folders-in-folder user zone path)) :files))))))


(defn- top-level-listing
  [{user :user}]
  (let [comm-f  (future (list-directories user (cfg/community-data)))
        share-f (future (list-directories user (cfg/irods-home)))
        home-f  (future (list-directories user (paths/user-home-dir user)))]
    {:roots [@home-f @comm-f @share-f]}))


(defn- shared-with-me-listing?
  [path]
  (= (ft/add-trailing-slash path)
     (ft/add-trailing-slash (cfg/irods-home))))


(defn do-directory
  [{:keys [user path] :or {path nil} :as params}]
  (cond
    (nil? path)                    (top-level-listing params)
    (shared-with-me-listing? path) (list-directories user (cfg/irods-home))
    :else                          (list-directories user path)))


(with-pre-hook! #'do-directory
  (fn [params]
    (paths/log-call "do-directory" params)
    (cv/validate-map params {:user string?})))

(with-post-hook! #'do-directory (paths/log-func "do-directory"))


(defn- total-filtered
  [user zone path]
  (icat/number-of-filtered-items-in-folder user
    zone
    path
    (apply str jv/bad-chars)
    (cfg/filter-files)
    (filtered-paths user)))


(defn- paged-dir-listing
  "Provides paged directory listing as an alternative to (list-dir). Always contains files."
  [user path limit offset scol sord]
  (log/info "paged-dir-listing - user:" user "path:" path "limit:" limit "offset:" offset)
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-readable cm user path)
    (validators/path-is-dir cm path)
    (let [stat  (item/stat cm path)
          zone  (cfg/irods-zone)
          pager (log/spy (icat/paged-folder-listing user zone path scol sord limit offset))]
      (assoc (page->map user pager)
        :id             (uuids/lookup-uuid cm path)
        :path           path
        :label          (paths/id->label user path)
        :filter         (should-filter? user path)
        :permission     (perm/permission-for cm user path)
        :hasSubDirs     true
        :date-created   (:date-created stat)
        :date-modified  (:date-modified stat)
        :file-size      0
        :total          (icat/number-of-items-in-folder user zone path)
        :total_filtered (total-filtered user zone path)))))


(defn- user-col->api-col
  [sort-col]
  (case (string/upper-case sort-col)
    "NAME"         :base-name
    "ID"           :full-path
    "LASTMODIFIED" :modify-ts
    "DATECREATED"  :create-ts
    "SIZE"         :data-size
    "PATH"         :full-path
    :base-name))


(defn- user-order->api-order
  [sort-order]
  (if (= "DESC" (string/upper-case sort-order))
    :desc
    :asc))


(defn- validate-sort-col
  [sort-col]
  (when-not (contains? #{"NAME" "ID" "LASTMODIFIED" "DATECREATED" "SIZE" "PATH"}
                       (string/upper-case sort-col))
    (log/warn "invalid sort column" sort-col)
    (throw+ {:error_code "ERR_INVALID_SORT_COLUMN" :column sort-col})))


(defn- validate-sort-order
  [sort-order]
  (when-not (contains? #{"ASC" "DESC"} (string/upper-case sort-order))
    (log/warn "invalid sort order" sort-order)
    (throw+ {:error_code "ERR_INVALID_SORT_ORDER" :sort-order sort-order})))


(defn do-paged-listing
  "Entrypoint for the API that calls (paged-dir-listing)."
  [{user       :user
    path       :path
    limit      :limit
    offset     :offset
    sort-col   :sort-col
    sort-order :sort-order}]
  (let [path       (ft/rm-last-slash path)
        limit      (Integer/parseInt limit)
        offset     (Integer/parseInt offset)
        sort-col   (user-col->api-col sort-col)
        sort-order (user-order->api-order sort-order)]
    (paged-dir-listing user path limit offset sort-col sort-order)))

(with-pre-hook! #'do-paged-listing
  (fn [params]
    (paths/log-call "do-paged-listing" params)
    (cv/validate-map params {:user   string?
                             :path   string?
                             :limit  string?
                             :offset string?})
    (when-let [col (:sort-col params)]
      (validate-sort-col col))
    (when-let [ord (:sort-order params)]
      (validate-sort-order ord))))

(with-post-hook! #'do-paged-listing (paths/log-func "do-paged-listing"))
