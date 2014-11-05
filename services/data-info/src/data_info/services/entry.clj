(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [me.raynes.fs :as fs]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as user]
            [clj-jargon.validations :as jv]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as file]
            [data-info.util.config :as cfg]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as duv])
  (:import [java.util UUID]))


(defn- canonicalize-str
  [str]
  (when str (str/lower-case str)))


(defn- path-handle-malformed
  [{missing-params ::missing-params}]
  {:error_code error/ERR_MISSING_QUERY_PARAMETER :parameters missing-params})


(defn- path-allowed?
  [irods path ctx]
  (let [user (get-in ctx [:request :params :user])]
    (if-not (user/user-exists? irods user)
      {::user-exists? false}
      (when (perm/is-readable? irods user path)
        {::user-exists? true}))))


;; id specific

(defn- id-allowed?
  [url-id user _]
  (try
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (if-not (user/user-exists? cm user)
        {::processable? false}
        (if-let [path (irods/get-path cm (UUID/fromString url-id))]
          (when (perm/is-readable? cm user path)
            {::processable? true ::exists? true})
          {::processable? true ::exists? false})))
    (catch IllegalArgumentException _
      {::processable? false})))


(defresource id-entry [url-id user]
  :allowed-methods       [:head]
  :allowed?              (partial id-allowed? url-id user)
  :exists?               #(::exists? %)
  :malformed?            (not user)
  :media-type-available? true
  :processable?          #(::processable? %))


;; file specific

(defn- file-malformed?
  [{req :request}]
  (when-not (contains? (:params req) :user)
    {::missing-params [:user]}))


(defn- resolve-attachment
  [attachment-param]
  (if-let [attachment-str (canonicalize-str attachment-param)]
    (when (contains? #{"true" "false"} attachment-str)
      (read-string attachment-str))
    false))


(defn- file-processable?
  [ctx]
  (when (::user-exists? ctx)
    (let [attachment (resolve-attachment (get-in ctx [:request :params :attachment]))]
      (when-not (nil? attachment)
        {::attachment attachment}))))


(defn- as-file-response
  [path data ctx]
  (if (and (map? data) (:error_code data))
    (rep/as-response data (assoc-in ctx [:representation :media-type] "application/json"))
    (let [filename    (str \" (file/basename path) \")
          disposition (if (::attachment ctx)
                        (str "attachment; filename=" filename)
                        (str "filename=" filename))]
      (assoc-in (rep/as-response data ctx) [:headers "Content-Disposition"] disposition))))


(defn- get-file
  [irods path]
  (if (zero? (item/file-size irods path))
    ""
    (ops/input-stream irods path)))


(defn- handle-unprocessable-file
  [ctx]
  (if-not (::user-exists? ctx)
    {:error_code error/ERR_NOT_A_USER
     :user       (get-in ctx [:request :params :user])}
    {:error_code error/ERR_BAD_QUERY_PARAMETER
     :parameters [:attachment]}))


(defresource file-entry [irods path]
  :allowed-methods             [:get]
  :available-media-types       [(irods/detect-media-type irods path)]
  :malformed?                  file-malformed?
  :allowed?                    (partial path-allowed? irods path)
  :processable?                file-processable?
  :as-response                 (partial as-file-response path)
  :handle-ok                   (get-file irods path)
  :handle-malformed            path-handle-malformed
  :handle-unprocessable-entity handle-unprocessable-file)


; folder specific

(defn- folder-malformed?
  [{req :request}]
  (let [missing-params (remove #(contains? (:params req) %) [:user :limit])]
    (when-not (empty? missing-params)
      {::missing-params missing-params})))


(defn- resolve-filter-names
  [filter-name-params]
  (cond
    (nil? filter-name-params)    #{}
    (string? filter-name-params) #{filter-name-params}
    :else                        (set filter-name-params)))


(defn- resolve-filter-paths
  [filter-path-params]
  (letfn [(fmt-path [path-param]
            (if (= "/" path-param)
              "/"
              (let [res (file/rm-last-slash
                          (if (= \/ (first path-param))
                            path-param
                            (str \/ path-param)))]
                (when-not (empty? res) res))))]
    (if (string? filter-path-params)
        #{(fmt-path filter-path-params)}
      (->> filter-path-params (map fmt-path) (remove nil?) set))))


(defn- resolve-nonnegative
  [nonnegative-param & [default]]
  (if-not nonnegative-param
    default
    (try
      (let [val (Long/parseLong nonnegative-param)]
        (when (>= val 0)
          val))
      (catch NumberFormatException _))))


(defn- resolve-sort-field
  [sort-field-param]
  (if-not sort-field-param
    :base-name
    (case (canonicalize-str sort-field-param)
      "datecreated"  :create-ts
      "datemodified" :modify-ts
      "name"         :base-name
      "path"         :full-path
      "size"         :data-size
      nil)))


(defn- resolve-sort-order
  [sort-order-param]
  (if-not sort-order-param
    :asc
    (case (canonicalize-str sort-order-param)
      "asc"  :asc
      "desc" :desc
      nil)))


(defn- resolve-folder-params
  [params]
  {::filter-chars (set (:filter-chars params))
   ::filter-names (resolve-filter-names (:filter-name params))
   ::filter-paths (resolve-filter-paths (:filter-path params))
   ::sort-field   (resolve-sort-field (:sort-field params))
   ::sort-order   (resolve-sort-order (:sort-order params))
   ::offset       (resolve-nonnegative (:offset params) 0)
   ::limit        (resolve-nonnegative (:limit params))})


(defn- folder-processable?
  [{user-exists? ::user-exists? req :request}]
  (when user-exists?
    (let [resolution (resolve-folder-params (:params req))]
      (when (not-any? nil? (vals resolution))
        resolution))))


(defn- fmt-entry
  [id date-created date-modified filter info-type path name permission size]
  {:id           (str id)
   :dateCreated  date-created
   :dateModified date-modified
   :filter       filter
   :infoType     info-type
   :name         name
   :path         path
   :permission   permission
   :size         size})


(defn- page-entry->map
  "Turns a entry in a paged listing result into a map containing file/directory information that can
   be consumed by the front-end."
  [filter?
   {:keys [access_type_id base_name create_ts data_size full_path info_type modify_ts uuid]}]
  (let [created  (* (Integer/parseInt create_ts) 1000)
        modified (* (Integer/parseInt modify_ts) 1000)
        filter   (filter? full_path)
        perm     (perm/fmt-perm access_type_id)]
    (fmt-entry uuid created modified filter info_type full_path base_name perm data_size)))


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
  [irods user path filter sfield sord offset limit]
  (let [id            (irods/lookup-uuid irods path)
        filter?       (should-filter? filter path)
        perm          (perm/permission-for irods user path)
        stat          (item/stat irods path)
        date-created  (:date-created stat)
        date-modified (:date-modified stat)
        zone          (cfg/irods-zone)
        name          (fs/base-name path)
        pager         (icat/paged-folder-listing user zone path sfield sord limit offset)]
    (merge (fmt-entry id date-created date-modified filter? nil path name perm 0)
           (page->map (partial should-filter? filter) pager)
           {:total         (icat/number-of-items-in-folder user zone path)
            :totalFiltered (total-filtered user zone path filter)})))


(defn- get-folder
  [irods path ctx]
  (let [user       (get-in ctx [:request :params :user])
        filter     {:chars (::filter-chars ctx)
                    :names (::filter-names ctx)
                    :paths (::filter-paths ctx)}
        sort-field (::sort-field ctx)
        sort-order (::sort-order ctx)
        offset     (::offset ctx)
        limit      (::limit ctx)]
    (paged-dir-listing irods user path filter sort-field sort-order offset limit)))


(defn- as-folder-response
  [data ctx]
  (if (and (map? data) (:error_code data))
    (rep/as-response data (assoc-in ctx [:representation :media-type] "application/json"))
    (rep/as-response data ctx)))


(defn- handle-unprocessable-folder
  [ctx]
  (if-not (::user-exists? ctx)
    {:error_code error/ERR_NOT_A_USER
     :user       (get-in ctx [:request :params :user])}
    (let [params     (get-in ctx [:request :params])
          resolution (resolve-folder-params params)
          bad-params (->> resolution
                       (filter #(nil? (second %)))
                       (map #(keyword (name (first %)))))]
      {:error_code error/ERR_BAD_QUERY_PARAMETER :parameters bad-params})))


(defresource folder-entry [irods path]
  :allowed-methods             [:get]
  :available-media-types       ["application/json"]
  :malformed?                  folder-malformed?
  :allowed?                    (partial path-allowed? irods path)
  :processable?                folder-processable?
  :as-response                 as-folder-response
  :handle-ok                   (partial get-folder irods path)
  :handle-malformed            path-handle-malformed
  :handle-unprocessable-entity handle-unprocessable-folder)


; TODO verify that each name in the path is not too large
(defn dispatch-path-to-resource
  [zone path-in-zone]
  (let [path (file/rm-last-slash (irods/abs-path zone path-in-zone))]
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (cond
        (> (count path) jv/max-path-length) {:status 414}
        (not (item/exists? cm path))        {:status 404}
        (item/is-dir? cm path)              (folder-entry cm path)
        :else                               (file-entry cm path)))))
