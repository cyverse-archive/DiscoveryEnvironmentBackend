(ns donkey.services.filesystem.directory
  (:use [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info]
        [clj-jargon.permissions]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [me.raynes.fs :as fs]
            [clojure-commons.error-codes :as error]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.persistence.metadata :as meta]
            [clj-icat-direct.icat :as icat]
            [donkey.clients.data-info :as data]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.common-paths :as paths]
            [donkey.services.filesystem.icat :as jargon])
  (:import [java.util UUID]
           [java.util.logging Filter]))


(defn get-paths-in-folder
  ([user folder]
    (get-paths-in-folder user folder (cfg/fs-max-paths-in-request)))

  ([user folder limit]
    (let [listing (icat/paged-folder-listing user (cfg/irods-zone) folder :base-name :asc limit 0)]
      (map :full_path listing))))

(defn- filtered-paths
  "Returns a seq of full paths that should not be included in paged listing."
  [user]
  [(cfg/fs-community-data)
   (ft/path-join (cfg/irods-home) user)
   (ft/path-join (cfg/irods-home) "public")])

(defn- should-filter?
  "Returns true if the map is okay to include in a directory listing."
  [user path-to-check]
  (let [fpaths (set (concat (cfg/fs-filter-files) (filtered-paths user)))]
    (or  (contains? fpaths path-to-check)
         (not (paths/valid-path? path-to-check)))))

(defn- page-entry->map
  "Turns a entry in a paged listing result into a map containing file/directory
   information that can be consumed by the front-end."
  [user {:keys [type full_path base_name data_size modify_ts create_ts access_type_id uuid]}]
  (let [base-map {:id            uuid
                  :path          full_path
                  :label         base_name
                  :isFavorite    (meta/is-favorite? user (UUID/fromString uuid))
                  :filter        (or (should-filter? user full_path)
                                     (should-filter? user base_name))
                  :file-size     data_size
                  :date-created  (* (Integer/parseInt create_ts) 1000)
                  :date-modified (* (Integer/parseInt modify_ts) 1000)
                  :permission    (fmt-perm access_type_id)}]
    (if (= type "dataobject")
      base-map
      (merge base-map {:hasSubDirs true
                       :file-size  0}))))

(defn- page->map
  "Transforms an entire page of results for a paged listing in a map that
   can be returned to the client."
  [user page]
  (let [entry-types (group-by :type page)
        do          (get entry-types "dataobject")
        collections (get entry-types "collection")
        xformer     (partial page-entry->map user)]
    {:files   (mapv xformer do)
     :folders (mapv xformer collections)}))


(defn- fmt-folder
  [user entry]
  (let [id   (:id entry)
        path (:path entry)]
    {:id            id
     :path          path
     :label         (paths/id->label user path)
     :isFavorite    (meta/is-favorite? user (UUID/fromString id))
     :filter        (or (should-filter? user path)
                        (should-filter? user (fs/base-name path)))
     :permission    (:permission entry)
     :date-created  (:dateCreated entry)
     :date-modified (:dateModified entry)
     :file-size     0
     :hasSubDirs    true}))


(defn- fmt-dir-resp
  [data-resp user]
  (assoc (fmt-folder user data-resp) :folders (map #(fmt-folder user %) (:folders data-resp))))


(defn- mk-nav-url
  [path]
  (let [nodes         (fs/split path)
        nodes         (if (= "/" (first nodes)) (next nodes) nodes)
        encoded-nodes (map url/url-encode nodes)]
    (apply url/url (cfg/data-info-base-url) "navigation" "path" encoded-nodes)))


(defn- list-directories
  "Lists the directories contained under path."
  [user path]
    (-> (http/get (str (mk-nav-url path)) {:query-params {:user user}})
      :body
      (json/decode true)
      :folder
      (fmt-dir-resp user)))


(defn- top-level-listing
  [{user :user}]
  (let [comm-f     (future (list-directories user (cfg/fs-community-data)))
        share-f    (future (list-directories user (cfg/irods-home)))
        home-f     (future (list-directories user (paths/user-home-dir user)))]
    {:roots [@home-f @comm-f @share-f]}))

(defn- shared-with-me-listing?
  [path]
  (= (ft/add-trailing-slash path) (ft/add-trailing-slash (cfg/irods-home))))


(defn do-directory
  [{:keys [user path] :or {path nil} :as params}]
  (cond
    (nil? path)
    (top-level-listing params)

    (shared-with-me-listing? path)
    (list-directories user (cfg/irods-home))

    :else
    (list-directories user path)))

(with-pre-hook! #'do-directory
  (fn [params]
    (paths/log-call "do-directory" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-directory (paths/log-func "do-directory"))


(defn- format-entry
  [user entry]
  (println entry)
  (let [id   (:id entry)
        path (:path entry)]
    {:id            id
     :path          path
     :label         (paths/id->label user path)
     :isFavorite    (meta/is-favorite? user (UUID/fromString id))
     :filter        (:filter entry)
     :permission    (:permission entry)
     :date-created  (:dateCreated entry)
     :date-modified (:dateModified entry)
     :file-size     (:size entry)}))


(defn- format-page
  [user page]
  (assoc (format-entry user page)
    :hasSubDirs     true
    :files          (map #(format-entry user %) (:files page))
    :folders        (map #(format-entry user %) (:folders page))
    :total          (:total page)
    :total_filtered (:totalFiltered page)))


(defn- paged-dir-listing
  "Provides paged directory listing as an alternative to (list-dir). Always contains files."
  [user path limit offset sort-field sort-order]
  (log/warn "paged-dir-listing - user:" user "path:" path "limit:" limit "offset:" offset)
  (let [url-path         (data/mk-entries-path-url-path path)
        req-map          {:query-params {:user         user
                                         :limit        limit
                                         :offset       offset
                                         :filter-chars (cfg/fs-filter-chars)
                                         :filter-name  (cfg/fs-filter-files)
                                         :filter-path  (filtered-paths user)
                                         :sort-field   sort-field
                                         :sort-order   sort-order}}
        handle-not-found (fn [_ _ _] (throw+ {:error_code error/ERR_NOT_FOUND :path path}))]
    (data/request :get url-path req-map
      :403 handle-not-found
      :404 handle-not-found
      :410 handle-not-found
      :414 handle-not-found)))


(defn- resolve-sort-field
  [sort-col]
  (if-not sort-col
    "NAME"
    (case (string/upper-case sort-col)
      "DATECREATED"  "DATECREATED"
      "ID"           "PATH"
      "LASTMODIFIED" "DATEMODIFIED"
      "NAME"         "NAME"
      "PATH"         "PATH"
      "SIZE"         "SIZE"
                     (do
                       (log/warn "invalid sort column" sort-col)
                       (throw+ {:error_code "ERR_INVALID_SORT_COLUMN" :column sort-col})))))


(defn- resolve-sort-order
  [sort-order]
  (if-not sort-order
    "ASC"
    (let [sort-order (string/upper-case sort-order)]
      (when-not (contains? #{"ASC" "DESC"} (string/upper-case sort-order))
        (log/warn "invalid sort order" sort-order)
        (throw+ {:error_code "ERR_INVALID_SORT_ORDER" :sort-order sort-order}))
      sort-order)))


; TODO validate limit >= 0, offset >= 0
(defn do-paged-listing
  "Entrypoint for the API that calls (paged-dir-listing)."
  [{user       :user
    path       :path
    limit      :limit
    offset     :offset
    sort-col   :sort-col
    sort-order :sort-order}]
  (Integer/parseInt limit)
  (Integer/parseInt offset)
  (let [path       (ft/rm-last-slash path)
        sort-field (resolve-sort-field sort-col)
        sort-order (resolve-sort-order sort-order)
        resp       (paged-dir-listing user path limit offset sort-field sort-order)]
    (format-page user (json/decode (:body resp) true))))

(with-pre-hook! #'do-paged-listing
  (fn [params]
    (paths/log-call "do-paged-listing" params)
    (validate-map params {:user string? :path string? :limit string? :offset string?})))

(with-post-hook! #'do-paged-listing (paths/log-func "do-paged-listing"))
