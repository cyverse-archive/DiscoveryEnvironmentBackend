(ns metadactyl.service.apps.jobs.submissions
  (:use [clojure-commons.core :only [remove-nil-values]]
        [slingshot.slingshot :only [try+ throw+]]
        [kameleon.uuids :only [uuid]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [kameleon.db :as db]
            [metadactyl.clients.data-info :as data-info]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.job-listings :as job-listings]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defn- get-app-params
  [app type-set]
  (->> (:groups app)
       (mapcat :parameters)
       (filter (comp type-set :type))
       (map (juxt (comp keyword :id) identity))
       (into {})))

(defn- get-file-stats
  [user paths]
  (try+
   (data-info/get-file-stats user paths)
   (catch Object _
     (log/error (:throwable &throw-context) "job submission failed")
     (throw+ {:error_code ce/ERR_REQUEST_FAILED
              :message "Could not lookup info types of inputs"}))))

(defn- load-path-list-stats
  [user input-paths-by-id]
  (->> (flatten (vals input-paths-by-id))
       (remove string/blank?)
       (get-file-stats user)
       (:paths)
       (map val)
       (filter (comp (partial = (config/path-list-info-type)) :infoType))))

(defn- param-value-contains-paths?
  [paths [_ v]]
  (if (sequential? v)
    (some (set paths) v)
    ((set paths) v)))

(defn- extract-ht-param-ids
  [path-list-stats input-paths-by-id]
  (let [ht-paths (set (map :path path-list-stats))]
    (map key (filter (partial param-value-contains-paths? ht-paths) input-paths-by-id))))

(defn- max-path-list-size-exceeded
  [max-size path actual-size]
  (throw+
   {:error_code ce/ERR_ILLEGAL_ARGUMENT
    :message    (str "HT Analysis Path List file exceeds maximum size of " max-size " bytes.")
    :path       path
    :file-size  actual-size}))

(defn- max-batch-paths-exceeded
  [max-paths first-list-path first-list-count]
  (throw+
   {:error_code ce/ERR_ILLEGAL_ARGUMENT
    :message    (str "The HT Analysis Path List exceeds the maximum of "
                     max-paths
                     " allowed paths.")
    :path       first-list-path
    :path-count first-list-count}))

(defn- validate-path-list-stats
  [{path :path actual-size :file-size}]
  (when (> actual-size (config/path-list-max-size))
    (max-path-list-size-exceeded (config/path-list-max-size) path actual-size)))

(defn- validate-ht-params
  [ht-params]
  (when (some (comp (partial = ap/param-multi-input-type) :type) ht-params)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message "HT Analysis Path List files are not supported in multi-file inputs."})))

(defn- validate-path-lists
  [path-lists]
  (let [[first-list-path first-list] (first path-lists)
        first-list-count             (count first-list)]
    (when (> first-list-count (config/path-list-max-paths))
      (max-batch-paths-exceeded (config/path-list-max-paths) first-list-path first-list-count))
    (when-not (every? (comp (partial = first-list-count) count second) path-lists)
      (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
               :message "All HT Analysis Path Lists must have the same number of paths."}))))

(defn- get-path-list-contents
  [user path]
  (try+
   (when (seq path) (data-info/get-path-list-contents user path))
   (catch [:status 500] {:keys [body]}
     (log/error (:throwable &throw-context) "job submission failed")
     (log/error (slurp body))
     (throw+ {:error_code ce/ERR_REQUEST_FAILED
              :message    "Could get file contents of path list input"}))
   (catch Object _
     (log/error (:throwable &throw-context) "job submission failed")
     (throw+ {:error_code ce/ERR_REQUEST_FAILED
              :message    "Could get file contents of path list input"}))))

(defn- get-path-list-contents-map
  [user paths]
  (into {} (map (juxt identity (partial get-path-list-contents user)) paths)))

(defn- get-batch-output-dir
  [user submission]
  (let [output-dir (ft/build-result-folder-path submission)]
    (try+
     (data-info/get-file-stats user [output-dir])
     (catch [:status 500] {:keys [body]}
       (if (= (:error_code (service/parse-json body)) ce/ERR_DOES_NOT_EXIST)
         (data-info/create-directory user output-dir)
         (throw+))))
    output-dir))

(defn- save-batch*
  [user app submission output-dir]
  (:id (jp/save-job {:job-name           (:name submission)
                     :description        (:description submission)
                     :app-id             (:app_id submission)
                     :app-name           (:name app)
                     :app-description    (:description app)
                     :result-folder-path output-dir
                     :start-date         (db/now)
                     :status             jp/submitted-status
                     :username           (:username user)
                     :notify             (:notify submission)}
                    submission)))

(defn- save-batch-step
  [batch-id job-type]
  (jp/save-job-step {:job-id          batch-id
                     :step-number     1
                     :status          jp/submitted-status
                     :app-step-number 1
                     :job-type        job-type}))

(defn- save-batch
  [user job-types app submission output-dir]
  (let [batch-id (save-batch* user app submission output-dir)]
    (save-batch-step batch-id (first job-types))
    batch-id))

(defn- map-slice
  [m n]
  (->> (map (fn [[k v]] (vector k (nth v n))) m)
       (into {})
       (remove-nil-values)))

(defn- map-slices
  [m]
  (let [max-count (apply max (map (comp count val) m))]
    (mapv (partial map-slice m) (range max-count))))


(defn- substitute-param-values
  [path-map config]
  (->> (map (fn [[k v]] (vector k (or (get path-map v v)))) config)
       (into {})))

(defn- format-submission-in-batch
  [submission job-number path-map]
  (let [job-suffix (str "analysis-" (inc job-number))]
    (assoc (update-in submission [:config] (partial substitute-param-values path-map))
      :name       (str (:name submission) "-" job-suffix)
      :output_dir (ft/path-join (:output_dir submission) job-suffix))))

(defn- submit-job-in-batch
  [apps-client submission job-number path-map]
  (.submitJob apps-client (format-submission-in-batch submission job-number path-map)))

(defn- preprocess-batch-submission
  [submission output-dir parent-id]
  (assoc submission
    :output_dir           output-dir
    :parent_id            parent-id
    :create_output_subdir false))

(defn- submit-batch-job
  [apps-client user input-params-by-id input-paths-by-id path-list-stats job-types app submission]
  (dorun (map validate-path-list-stats path-list-stats))
  (let [ht-param-ids (extract-ht-param-ids path-list-stats input-paths-by-id)
        _            (validate-ht-params (vals (select-keys input-params-by-id ht-param-ids)))
        ht-paths     (set (map :path path-list-stats))
        path-lists   (get-path-list-contents-map user ht-paths)
        _            (validate-path-lists path-lists)
        path-maps    (map-slices path-lists)
        output-dir   (get-batch-output-dir user submission)
        batch-id     (save-batch user job-types app submission output-dir)
        submission   (preprocess-batch-submission submission output-dir batch-id)]
    (dorun (map-indexed (partial submit-job-in-batch apps-client submission) path-maps))
    (job-listings/list-job apps-client batch-id)))

(defn submit
  [apps-client user submission]
  (let [[job-types app]    (.getAppSubmissionInfo apps-client (:app_id submission))
        input-params-by-id (get-app-params app ap/param-ds-input-types)
        input-paths-by-id  (select-keys (:config submission) (keys input-params-by-id))]
    (if-let [path-list-stats (seq (load-path-list-stats user input-paths-by-id))]
      (submit-batch-job apps-client user input-params-by-id input-paths-by-id
                        (log/spy :warn path-list-stats) job-types app submission)
      (.submitJob apps-client submission))))
