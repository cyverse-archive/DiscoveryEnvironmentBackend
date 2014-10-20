(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [liberator.core :refer [defresource]]
            [slingshot.slingshot :refer [throw+ try+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as user]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as file]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as duv])
  (:import [java.util UUID]))


(defn- allowed?
  [url-id user _]
  (try+
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (if-not (user/user-exists? cm user)
        {::processable? false}
        (if-let [path (irods/get-path cm (UUID/fromString url-id))]
          (when (perm/is-readable? cm user path)
            {::processable? true ::exists? true})
          {::processable? true ::exists? false})))
    (catch IllegalArgumentException _
      {::processable? false})))


(defresource entry [url-id user]
  :allowed-methods       [:head]
  :allowed?              (partial allowed? url-id user)
  :exists?               #(::exists? %)
  :malformed?            (fn [_] (not user))
  :media-type-available? (fn [_] true)
  :processable?          #(::processable? %))


(defn- download-file
  [user file-path]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (duv/path-readable cm user file-path)
    (if (zero? (item/file-size cm file-path))
      ""
      (ops/input-stream cm file-path))))


(defn- get-disposition
  [path attachment]
  (let [filename (str \" (file/basename path) \")]
    (if (or (nil? attachment) (Boolean/parseBoolean attachment))
      (str "attachment; filename=" filename)
      (str "filename=" filename))))


(defn- get-file
  [path {:keys [attachment user]}]
  (let [content-type (future (irods/detect-media-type path))]
    {:status  200
     :body    (download-file user path)
     :headers {"Content-Disposition" (get-disposition path attachment)
               "Content-Type"        @content-type}}))

(with-pre-hook! #'get-file
  (fn [path params]
    (dul/log-call "get-file" path params)
    (cv/validate-map params {:user string?})
    (when-let [attachment (:attachment params)]
      (duv/valid-bool-param "attachment" attachment))
    (log/info "User for download: " (:user params))
    (log/info "Path to download: " path)))

(with-post-hook! #'get-file (dul/log-func "get-file"))


(defn- fmt-entry
  [id date-created date-modified filter path permission size]
  {:id           id
   :dateCreated  date-created
   :dateModified date-modified
   :filter       filter
   :path         path
   :permission   permission
   :size         size})


(defn- page-entry->map
  "Turns a entry in a paged listing result into a map containing file/directory information that can
   be consumed by the front-end."
  [filter? {:keys [access_type_id create_ts data_size full_path modify_ts uuid]}]
  (let [created  (* (Integer/parseInt create_ts) 1000)
        modified (* (Integer/parseInt modify_ts) 1000)
        filter   (filter? full_path)
        perm     (perm/fmt-perm access_type_id)]
    (fmt-entry uuid created modified filter full_path perm data_size)))


(defn- page->map
  "Transforms an entire page of results for a paged listing in a map that can be returned to the
   client."
  [filter? page]
  (let [entry-types (group-by :type page)
        do          (get entry-types "dataobject")
        collections (get entry-types "collection")
        xformer     (partial page-entry->map filter?)]
    {:files   (mapv xformer do)
     :folders (mapv xformer collections)}))


(defn- should-filter?
  "Returns true if the map is okay to include in a directory listing."
  [filter path]
  (or (contains? (:paths filter) path)
      (contains? (:names filter) (file/basename path))
      (not (duv/good-string? (:chars filter) path))))


(defn- total-filtered
  [user zone parent filter]
  (let [cfilt (apply str (:chars filter))
        nfilt (:names filter)
        pfilt (:paths filter)]
    (icat/number-of-filtered-items-in-folder user zone parent cfilt nfilt pfilt)))


(defn- paged-dir-listing
  "Provides paged directory listing as an alternative to (list-dir). Always contains files."
  [user path limit offset sfield sord filters]
  (log/info "paged-dir-listing - user:" user "path:" path "limit:" limit "offset:" offset)
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (duv/user-exists cm user)
    (duv/path-readable cm user path)
    (let [id      (irods/lookup-uuid cm path)
          filter  (should-filter? filters path)
          perm    (perm/permission-for cm user path)
          stat    (item/stat cm path)
          zone    (cfg/irods-zone)
          pager   (icat/paged-folder-listing user zone path sfield sord limit offset)]
      (merge (fmt-entry id (:date-created stat) (:date-modified stat) filter path perm 0)
             (page->map (partial should-filter? filters) pager)
             {:total         (icat/number-of-items-in-folder user zone path)
              :totalFiltered (total-filtered user zone path filters)}))))


(def ^:private api-field->db-col
  {"datecreated"  :create-ts
   "datemodified" :modify-ts
   "name"         :base-name
   "path"         :full-path
   "size"         :data-size
   nil            :base-name})


(def ^:private api-order->db-order
  {"asc"  :asc
   "desc" :desc
   nil    :asc})


(defn- canonicalize-str
  [str]
  (when str (str/lower-case str)))


(defn- resolve-str-set
  [str-vals]
  (cond
    (nil?    str-vals) #{}
    (string? str-vals) #{str-vals}
    :else              (set str-vals)))


(defn- resolve-sort-field
  [sort-field]
  (get api-field->db-col (canonicalize-str sort-field)))


(defn- resolve-sort-order
  [sort-order]
  (get api-order->db-order (canonicalize-str sort-order)))


(defn- validate-sort-field
  [sort-field]
  (when-not (contains? api-field->db-col (canonicalize-str sort-field))
    (log/warn "invalid sort field" sort-field)
    (throw+ {:error_code error/ERR_BAD_QUERY_PARAMETER
             :parameter  "sort-field"
             :value      sort-field})))


(defn- validate-sort-order
  [sort-order]
  (when-not (contains? api-order->db-order (canonicalize-str sort-order))
    (log/warn "invalid sort order" sort-order)
    (throw+ {:error_code error/ERR_BAD_QUERY_PARAMETER
             :parameter  "sort-order"
             :value      sort-order})))


(defn- get-folder
  [path {:keys [filter-chars filter-name filter-path limit offset sort-field sort-order user]}]
  (let [path       (file/rm-last-slash path)
        filter     {:chars (set filter-chars)
                    :names (resolve-str-set filter-name)
                    :paths (resolve-str-set filter-path)}
        limit      (Integer/parseInt limit)
        offset     (Integer/parseInt offset)
        sort-field (resolve-sort-field sort-field)
        sort-order (resolve-sort-order sort-order)]
    (paged-dir-listing user path limit offset sort-field sort-order filter)))

(with-pre-hook! #'get-folder
  (fn [path params]
    (dul/log-call "get-folder" path params)
    (cv/validate-map params {:limit  cv/nonnegative-int?
                             :offset cv/nonnegative-int?
                             :user   string?})
    (when-let [field (:sort-field params)]
      (validate-sort-field field))
    (when-let [order (:sort-order params)]
      (validate-sort-order order))))

(with-post-hook! #'get-folder (dul/log-func "get-folder"))


(defn get-by-path
  [path-in-zone {zone :zone :as params}]
  (let [full-path (irods/abs-path zone path-in-zone)

        ;; detecting if the path is a folder happens in a separate connection to iRODS on purpose.
        ;; It appears that downloading a file after detecting its type causes the download to fail.
        ; TODO after migrating to jargon 4, check to see if this error still occurs.
        folder? (init/with-jargon (cfg/jargon-cfg) [cm]
                  (duv/path-exists cm full-path)
                  (item/is-dir? cm full-path))]
    (if folder?
      (get-folder full-path params)
      (get-file full-path params))))
