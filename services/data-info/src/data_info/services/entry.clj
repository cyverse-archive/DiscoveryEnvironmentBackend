(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [liberator.core :refer [defresource]]
            [liberator.representation :as rep]
            [slingshot.slingshot :refer [try+]]
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


(defn- id-allowed?
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


(defresource id-entry [url-id user]
  :allowed-methods       [:head]
  :allowed?              (partial id-allowed? url-id user)
  :exists?               #(::exists? %)
  :malformed?            (not user)
  :media-type-available? true
  :processable?          #(::processable? %))


(defn- available-media-types
  [zone path-in-zone ctx]
  (when (::exists? ctx)
    (let [path (irods/abs-path zone path-in-zone)]
      (init/with-jargon (cfg/jargon-cfg) [cm]
        (if (item/is-dir? cm path)
          ["application/json"]
          [(irods/detect-media-type cm path)])))))


(defn- uri-too-long?
  [zone path-in-zone _]
  (> (count (irods/abs-path zone path-in-zone)) jv/max-path-length))


(defn- malformed?
  [zone path-in-zone {req :request}]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [full-path      (irods/abs-path zone path-in-zone)
          req-params     (if (and (item/exists? cm full-path)
                                  (item/is-dir? cm full-path))
                           [:user :limit]
                           [:user])
          missing-params (remove #(contains? (:params req) %) req-params)]
      (when-not (empty? missing-params)
        {:representation {:media-type "application/json"}
         :message        {:error_code error/ERR_MISSING_QUERY_PARAMETER
                          :parameters missing-params}}))))


(defn- path-allowed?
  [zone path-in-zone ctx]
  (let [path (irods/abs-path zone path-in-zone)
        user (get-in ctx [:request :params :user])]
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (if-not (user/user-exists? cm user)
        {::user-exists? false}
        (if-not (item/exists? cm path)
          {::exists? false}
          (when (perm/is-readable? cm user path)
            {::exists? true}))))))


(defn- canonicalize-str
  [str]
  (when str (str/lower-case str)))


(defn- resolve-attachment
  [attachment-param]
  (if-let [attachment-str (canonicalize-str attachment-param)]
    (when (contains? #{"true" "false"} attachment-str)
      (read-string attachment-str))
    false))


(defn- resolve-file-params
  [path params]
  {::entry-type :file
   ::path       path
   ::attachment (resolve-attachment (:attachment params))})


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
    (try+
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
  [path params]
  {::entry-type   :folder
   ::path         path
   ::filter-chars (set (:filter-chars params))
   ::filter-names (resolve-filter-names (:filter-name params))
   ::filter-paths (resolve-filter-paths (:filter-path params))
   ::sort-field   (resolve-sort-field (:sort-field params))
   ::sort-order   (resolve-sort-order (:sort-order params))
   ::offset       (resolve-nonnegative (:offset params) 0)
   ::limit        (resolve-nonnegative (:limit params))})


(defn- processable?
  [zone path-in-zone ctx]
  (if (false? (::exists? ctx))
    true
    (when-not (false? (::user-exists? ctx))
      (init/with-jargon (cfg/jargon-cfg) [cm]
        (let [params     (get-in ctx [:request :params])
              path       (irods/abs-path zone path-in-zone)
              resolution (if (item/is-dir? cm path)
                           (resolve-folder-params path params)
                           (resolve-file-params path params))]
          (when (not-any? nil? (vals resolution))
            resolution))))))


(defn- handle-unprocessable-entity
  [zone path-in-zone ctx]
  (if (false? (::user-exists? ctx))
    {:error_code error/ERR_NOT_A_USER
     :user       (get-in ctx [:request :params :user])}
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (let [path       (irods/abs-path zone path-in-zone)
            params     (get-in ctx [:request :params])
            resolution (if (item/is-dir? cm path)
                         (resolve-folder-params path params)
                         (resolve-file-params path params))
            bad-params (->> resolution
                         (filter #(nil? (second %)))
                         (map #(keyword (name (first %)))))]
        {:error_code error/ERR_BAD_QUERY_PARAMETER :parameters bad-params}))))


(defn- as-response
  [data ctx]
  (if (and (map? data) (:error_code data))
    (rep/as-response data (assoc-in ctx [:representation :media-type] "application/json"))
    (let [fmt-disposition (fn []
                            (let [filename (str \" (file/basename (::path ctx)) \")]
                              (if (::attachment ctx)
                                (str "attachment; filename=" filename)
                                (str "filename=" filename))))
          response        (rep/as-response data ctx)]
      (if (= :file (::entry-type ctx))
        (assoc-in response [:headers "Content-Disposition"] (fmt-disposition))
        response))))


(defn- get-file
  [{path ::path}]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (if (zero? (item/file-size cm path))
      ""
      (ops/input-stream cm path))))


(defn- fmt-entry
  [id date-created date-modified filter path permission size]
  {:id           (str id)
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
  [user path filter sfield sord offset limit]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [id      (irods/lookup-uuid cm path)
          filter? (should-filter? filter path)
          perm    (perm/permission-for cm user path)
          stat    (item/stat cm path)
          zone    (cfg/irods-zone)
          pager   (icat/paged-folder-listing user zone path sfield sord limit offset)]
      (merge (fmt-entry id (:date-created stat) (:date-modified stat) filter? path perm 0)
        (page->map (partial should-filter? filter) pager)
        {:total         (icat/number-of-items-in-folder user zone path)
         :totalFiltered (total-filtered user zone path filter)}))))


(defn- get-folder
  [ctx]
  (let [user       (get-in ctx [:request :params :user])
        path       (::path ctx)
        filter     {:chars (::filter-chars ctx)
                    :names (::filter-names ctx)
                    :paths (::filter-paths ctx)}
        sort-field (::sort-field ctx)
        sort-order (::sort-order ctx)
        offset     (::offset ctx)
        limit      (::limit ctx)]
    (paged-dir-listing user path filter sort-field sort-order offset limit)))


(defn- handle-ok
  [ctx]
  (case (::entry-type ctx)
    :file   (get-file   ctx)
    :folder (get-folder ctx)))


(defresource path-entry [zone path-in-zone]
  :allowed-methods             [:get]
  :available-media-types       (partial available-media-types zone path-in-zone)
  :uri-too-long?               (partial uri-too-long? zone path-in-zone)
  :malformed?                  (partial malformed? zone path-in-zone)
  :allowed?                    (partial path-allowed? zone path-in-zone)
  :processable?                (partial processable? zone path-in-zone)
  :exists?                     ::exists?
  :as-response                 as-response
  :handle-ok                   handle-ok
  :handle-malformed            #(json/encode (:message %))
  :handle-unprocessable-entity (partial handle-unprocessable-entity zone path-in-zone))