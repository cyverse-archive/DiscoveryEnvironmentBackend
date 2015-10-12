(ns donkey.services.filesystem.directory
  (:use [clojure-commons.validators]
        [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [me.raynes.fs :as fs]
            [clojure-commons.error-codes :as error]
            [clj-icat-direct.icat :as icat]
            [donkey.clients.data-info :as data]
            [donkey.services.metadata.favorites :as favorites]
            [donkey.util.config :as cfg]
            [donkey.util.validators :as duv]
            [donkey.services.filesystem.common-paths :as paths]))

(defn- is-favorite?
  [favorite-ids id]
  (contains? favorite-ids (uuidify id)))

(defn- lookup-favorite-ids
  "Filters the list of given data IDs, returning those marked as favorites by the user according to
  the metadata filter-favorites service. If the filtered list of favorite IDs cannot be retrieved,
  an empty list is returned instead."
  [data-ids]
  (try+
    (favorites/filter-favorites data-ids)
    (catch Object e
      (log/error e "Could not lookup favorites in directory listing")
      [])))

(defn get-paths-in-folder
  ([user folder]
    (get-paths-in-folder user folder (cfg/fs-max-paths-in-request)))

  ([user folder limit]
    (let [listing (icat/paged-folder-listing
                    :user           user
                    :zone           (cfg/irods-zone)
                    :folder-path    folder
                    :entity-type    :any
                    :sort-column    :base-name
                    :sort-direction :asc
                    :limit          limit
                    :offset         0
                    :info-types     nil)]
      (map :full_path listing))))


(defn- bad-paths
  "Returns a seq of full paths that should not be included in paged listing."
  [user]
  [(cfg/fs-community-data)
   (ft/path-join (cfg/irods-home) user)
   (ft/path-join (cfg/irods-home) "public")])

(defn- is-bad?
  "Returns true if the map is okay to include in a directory listing."
  [user path-to-check]
  (let [fpaths (set (concat (cfg/fs-bad-names) (bad-paths user)))]
    (or  (contains? fpaths path-to-check)
         (not (paths/valid-path? path-to-check)))))


(defn- fmt-folder
  [user favorite-ids data-item]
  (let [id   (:id data-item)
        path (:path data-item)]
    {:id            id
     :path          path
     :label         (paths/id->label user path)
     :isFavorite    (is-favorite? favorite-ids id)
     :badName       (or (is-bad? user path)
                        (is-bad? user (fs/base-name path)))
     :permission    (:permission data-item)
     :date-created  (:dateCreated data-item)
     :date-modified (:dateModified data-item)
     :file-size     0
     :hasSubDirs    true}))


(defn- fmt-dir-resp
  [{:keys [id folders] :as data-resp} user]
  (let [favorite-ids (->> folders
                          (map :id)
                          (concat [id])
                          lookup-favorite-ids)]
    (assoc (fmt-folder user favorite-ids data-resp)
      :folders (map (partial fmt-folder user favorite-ids) folders))))


(defn- mk-nav-url
  [path]
  (let [nodes         (fs/split path)
        nodes         (if (= "/" (first nodes)) (next nodes) nodes)
        encoded-nodes (map url/url-encode nodes)]
    (apply url/url (cfg/data-info-base) "navigation" "path" encoded-nodes)))


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


(defn- format-data-item
  [user favorite-ids data-item]
  (let [id   (:id data-item)
        path (:path data-item)]
    {:id            id
     :path          path
     :label         (paths/id->label user path)
     :infoType      (:infoType data-item)
     :isFavorite    (is-favorite? favorite-ids id)
     :badName       (:badName data-item)
     :permission    (:permission data-item)
     :date-created  (:dateCreated data-item)
     :date-modified (:dateModified data-item)
     :file-size     (:size data-item)}))


(defn- format-page
  [user {:keys [id files folders total totalBad] :as page}]
  (let [file-ids (map :id files)
        folder-ids (map :id folders)
        favorite-ids (lookup-favorite-ids (concat file-ids folder-ids [id]))]
    (assoc (format-data-item user favorite-ids page)
      :hasSubDirs true
      :files      (map (partial format-data-item user favorite-ids) files)
      :folders    (map (partial format-data-item user favorite-ids) folders)
      :total      total
      :totalBad   totalBad)))


(defn- handle-not-processable
  [method url err]
  (let [body (json/decode (:body err) true)]
    (if (and (= (:error_code body) error/ERR_BAD_QUERY_PARAMETER)
             (= (:parameters body) ["info-type"]))
      (throw+ body)
      (data/respond-with-default-error 422 method url err))))


(defn- paged-dir-listing
  "Provides paged directory listing as an alternative to (list-dir). Always contains files."
  [user path entity-type limit offset sort-field sort-dir info-type]
  (log/info "paged-dir-listing - user:" user "path:" path "limit:" limit "offset:" offset)
  (let [url-path         (data/mk-data-path-url-path path)
        params           {:user        user
                          :entity-type (name entity-type)
                          :limit       limit
                          :offset      offset
                          :bad-chars   (cfg/fs-bad-chars)
                          :bad-name    (cfg/fs-bad-names)
                          :bad-path    (bad-paths user)
                          :sort-field  sort-field
                          :sort-dir    sort-dir}
        params           (if info-type
                           (assoc params :info-type info-type)
                           params)
        handle-not-found (fn [_ _ _] (throw+ {:error_code error/ERR_NOT_FOUND :path path}))]
    (data/request :get url-path {:query-params params}
      :403 handle-not-found
      :404 handle-not-found
      :410 handle-not-found
      :414 handle-not-found
      :422 handle-not-processable)))


(defn- resolve-sort-field
  [sort-col]
  (if-not sort-col
    "name"
    (case (string/lower-case sort-col)
      "datecreated"  "datecreated"
      "id"           "path"
      "lastmodified" "datemodified"
      "name"         "name"
      "path"         "path"
      "size"         "size"
                     (do
                       (log/warn "invalid sort column" sort-col)
                       (throw+ {:error_code "ERR_INVALID_SORT_COLUMN" :column sort-col})))))


(defn- resolve-sort-dir
  [sort-dir]
  (if-not sort-dir
    "ASC"
    (let [sort-dir (string/upper-case sort-dir)]
      (when-not (contains? #{"ASC" "DESC"} (string/upper-case sort-dir))
        (log/warn "invalid sort order" sort-dir)
        (throw+ {:error_code "ERR_INVALID_SORT_DIR" :sort-dir sort-dir}))
      sort-dir)))


; TODO validate limit >= 0, offset >= 0
(defn do-paged-listing
  "Entrypoint for the API that calls (paged-dir-listing)."
  [{:keys [user path entity-type info-type limit offset sort-col sort-dir]}]
  (Integer/parseInt limit)
  (Integer/parseInt offset)
  (let [path        (ft/rm-last-slash path)
        entity-type (duv/resolve-entity-type entity-type)
        sort-field  (resolve-sort-field sort-col)
        sort-dir    (resolve-sort-dir sort-dir)
        resp        (paged-dir-listing
                      user path entity-type limit offset sort-field sort-dir info-type)]
    (format-page user (json/decode (:body resp) true))))

(with-pre-hook! #'do-paged-listing
  (fn [params]
    (paths/log-call "do-paged-listing" params)
    (validate-map params {:user string? :path string? :limit string? :offset string?})))

(with-post-hook! #'do-paged-listing (paths/log-func "do-paged-listing"))
