(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [clojure.string :as str]
            [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [me.raynes.fs :as fs]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.by-uuid :as uuid]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as user]
            [clj-jargon.validations :as jv]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as file]
            [heuristomancer.core :as info]
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
  [attrs ctx]
  (if-not (:user-exists? attrs)
    {::user-exists? false}
    (when (:path-readable? attrs)
      {::user-exists? true})))


;; id specific

(defn- id-allowed?
  [url-id user _]
  (try
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (if-not (user/user-exists? cm user)
        {::processable? false}
        (if-let [path (uuid/get-path cm (UUID/fromString url-id))]
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
  [path]
  (init/with-jargon (cfg/jargon-cfg) [irods]
    (if (zero? (item/file-size irods path))
      ""
      (ops/input-stream irods path))))


(defn- handle-unprocessable-file
  [ctx]
  (if-not (::user-exists? ctx)
    {:error_code error/ERR_NOT_A_USER
     :user       (get-in ctx [:request :params :user])}
    {:error_code error/ERR_BAD_QUERY_PARAMETER
     :parameters [:attachment]}))


(defresource file-entry [{:keys [path] :as attrs}]
  :allowed-methods             [:get]
  :available-media-types       [(:media-type attrs)]
  :malformed?                  file-malformed?
  :allowed?                    (partial path-allowed? attrs)
  :processable?                file-processable?
  :as-response                 (partial as-file-response path)
  :handle-ok                   (get-file path)
  :handle-malformed            path-handle-malformed
  :handle-unprocessable-entity handle-unprocessable-file)


; folder specific

(defn- folder-malformed?
  [{req :request}]
  (let [missing-params (remove #(contains? (:params req) %) [:user :limit])]
    (when-not (empty? missing-params)
      {::missing-params missing-params})))


(defn- resolve-bad-names
  [bad-name-params]
  (cond
    (nil? bad-name-params)    #{}
    (string? bad-name-params) #{bad-name-params}
    :else                     (set bad-name-params)))


(defn- resolve-bad-paths
  [bad-path-params]
  (letfn [(fmt-path [path-param]
            (if (= "/" path-param)
              "/"
              (let [res (file/rm-last-slash
                          (if (= \/ (first path-param))
                            path-param
                            (str \/ path-param)))]
                (when-not (empty? res) res))))]
    (if (string? bad-path-params)
      #{(fmt-path bad-path-params)}
      (->> bad-path-params (map fmt-path) (remove nil?) set))))


(defn- resolve-entity-type
  [entity-type-param]
  (if (empty? entity-type-param)
    :any
    (case (str/lower-case entity-type-param)
      "any"    :any
      "file"   :file
      "folder" :folder
               nil)))


(defn- resolve-info-types
  [info-type-params]
  (let [resolve-type     (fn [param] (when (some #(= param %) (info/supported-formats))
                                       param))
        info-type-params (cond
                           (nil? info-type-params)    []
                           (string? info-type-params) [info-type-params]
                           :else                      info-type-params)
        resolution       (map resolve-type info-type-params)]
    (when (not-any? nil? resolution)
      resolution)))


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
  {::entity-type (resolve-entity-type (:entity-type params))
   ::bad-chars   (set (:bad-chars params))
   ::bad-name    (resolve-bad-names (:bad-name params))
   ::bad-path    (resolve-bad-paths (:bad-path params))
   ::info-type   (resolve-info-types (:info-type params))
   ::sort-field  (resolve-sort-field (:sort-field params))
   ::sort-order  (resolve-sort-order (:sort-order params))
   ::offset      (resolve-nonnegative (:offset params) 0)
   ::limit       (resolve-nonnegative (:limit params))})


(defn- folder-processable?
  [{user-exists? ::user-exists? req :request}]
  (when user-exists?
    (let [resolution (resolve-folder-params (:params req))]
      (when (not-any? nil? (vals resolution))
        resolution))))


(defn- fmt-entry
  [id date-created date-modified bad? info-type path name permission size]
  {:id           (str id)
   :dateCreated  date-created
   :dateModified date-modified
   :badName      bad?
   :infoType     info-type
   :name         name
   :path         path
   :permission   permission
   :size         size})


(defn- page-entry->map
  "Turns a entry in a paged listing result into a map containing file/directory information that can
   be consumed by the front-end."
  [mark-bad?
   {:keys [access_type_id base_name create_ts data_size full_path info_type modify_ts uuid]}]
  (let [created  (* (Integer/parseInt create_ts) 1000)
        modified (* (Integer/parseInt modify_ts) 1000)
        bad?     (mark-bad? full_path)
        perm     (perm/fmt-perm access_type_id)]
    (fmt-entry uuid created modified bad? info_type full_path base_name perm data_size)))


(defn- page->map
  "Transforms an entire page of results for a paged listing in a map that can be returned to the
   client."
  [mark-bad? page]
  (let [entry-types (group-by :type page)
        do          (get entry-types "dataobject")
        collections (get entry-types "collection")
        xformer     (partial page-entry->map mark-bad?)]
    {:files   (mapv xformer do)
     :folders (mapv xformer collections)}))


(defn- is-bad?
  "Returns true if the map is okay to include in a directory listing."
  [bad-indicator path]
  (let [basename (file/basename path)]
    (or (contains? (:paths bad-indicator) path)
        (contains? (:names bad-indicator) basename)
        (not (duv/good-string? (:chars bad-indicator) basename)))))


(defn- total-bad
  [user zone parent entity-type info-types bad-indicator]
  (icat/number-of-bad-items-in-folder
    :user user
    :zone zone
    :parent-path parent
    :entity-type entity-type
    :info-types  info-types
    :bad-chars   (apply str (:chars bad-indicator))
    :bad-names   (:names bad-indicator)
    :bad-paths   (:paths bad-indicator)))


(defn- paged-dir-listing
  "Provides paged directory listing as an alternative to (list-dir). Always contains files."
  [irods user path entity-type bad-indicator sfield sord offset limit info-types]
  (let [id           (irods/lookup-uuid irods path)
        bad?         (is-bad? bad-indicator path)
        perm         (perm/permission-for irods user path)
        stat         (item/stat irods path)
        date-created (:date-created stat)
        mod-date     (:date-modified stat)
        zone         (cfg/irods-zone)
        name         (fs/base-name path)
        page         (icat/paged-folder-listing
                       :user           user
                       :zone           zone
                       :folder-path    path
                       :entity-type    entity-type
                       :info-types     info-types
                       :sort-column    sfield
                       :sort-direction sord
                       :limit          limit
                       :offset         offset)]
    (merge (fmt-entry id date-created mod-date bad? nil path name perm 0)
           (page->map (partial is-bad? bad-indicator) page)
           {:total    (icat/number-of-items-in-folder user zone path entity-type info-types)
            :totalBad (total-bad user zone path entity-type info-types bad-indicator)})))


(defn- get-folder
  [path ctx]
  (let [user        (get-in ctx [:request :params :user])
        entity-type (::entity-type ctx)
        badies      {:chars (::bad-chars ctx)
                     :names (::bad-name ctx)
                     :paths (::bad-path ctx)}
        sort-field  (::sort-field ctx)
        sort-order  (::sort-order ctx)
        offset      (::offset ctx)
        limit       (::limit ctx)
        info-types  (::info-type ctx)]
    (init/with-jargon (cfg/jargon-cfg) [irods]
      (paged-dir-listing irods user path entity-type badies sort-field sort-order offset limit
                         info-types))))


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


(defresource folder-entry [{:keys [path] :as attrs}]
  :allowed-methods             [:get]
  :available-media-types       [(:media-type attrs)]
  :malformed?                  folder-malformed?
  :allowed?                    (partial path-allowed? attrs)
  :processable?                folder-processable?
  :as-response                 as-folder-response
  :handle-ok                   (partial get-folder path)
  :handle-malformed            path-handle-malformed
  :handle-unprocessable-entity handle-unprocessable-folder)


(defn- get-media-type
  [cm path is-dir?]
  (if is-dir?
    "application/json"
    (irods/detect-media-type cm path)))


(defn- get-path-attrs
  [zone path-in-zone req]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [path    (file/rm-last-slash (irods/abs-path zone path-in-zone))
          exists? (item/exists? cm path)
          is-dir? (and exists? (item/is-dir? cm path))
          user    (get-in req [:params :user])]
      {:path           path
       :exists?        exists?
       :is-dir?        is-dir?
       :user-exists?   (user/user-exists? cm user)
       :path-readable? (perm/is-readable? cm user path)
       :media-type     (when exists? (get-media-type cm path is-dir?))})))


; TODO verify that each name in the path is not too large
(defn dispatch-path-to-resource
  [zone path-in-zone req]
  (let [{:keys [path exists? is-dir?] :as attrs} (get-path-attrs zone path-in-zone req)]
    (cond
     (> (count path) jv/max-path-length) {:status 414}
     (not exists?)                       {:status 404}
     is-dir?                             (folder-entry attrs)
     :else                               (file-entry attrs))))
