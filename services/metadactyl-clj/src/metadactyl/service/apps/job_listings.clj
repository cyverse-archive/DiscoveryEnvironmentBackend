(ns metadactyl.service.apps.job-listings
  (:use [kameleon.uuids :only [uuidify]]
        [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [kameleon.db :as db]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.util :as util]))

(defn is-completed?
  [job-status]
  (jp/completed-status-codes job-status))

(defn is-running?
  [job-status]
  (= jp/running-status job-status))

(def not-completed? (complement is-completed?))

(defn- job-timestamp
  [timestamp]
  (str (or (db/millis-from-timestamp timestamp) 0)))

(defn- app-disabled?
  [app-tables app-id]
  (let [disabled-flag (:disabled (first (remove nil? (map #(% app-id) app-tables))))]
    (if (nil? disabled-flag) true disabled-flag)))

(defn- batch-child-status
  [{:keys [status]}]
  (cond (is-completed? status) :completed
        (is-running? status)   :running
        :else                  :submitted))

(def ^:private empty-batch-child-status
  {:total     0
   :completed 0
   :running   0
   :submitted 0})

(defn- format-batch-status
  [batch-id]
  (merge empty-batch-child-status
         (let [children (jp/list-child-jobs batch-id)]
           (assoc (->> (group-by batch-child-status children)
                       (map (fn [[k v]] [k (count v)]))
                       (into {}))
             :total (count children)))))

(defn format-job
  [app-tables job]
  (remove-nil-vals
   {:app_description (:app-description job)
    :app_id          (:app-id job)
    :app_name        (:app-name job)
    :description     (:description job)
    :enddate         (job-timestamp (:end-date job))
    :id              (:id job)
    :name            (:job-name job)
    :resultfolderid  (:result-folder-path job)
    :startdate       (job-timestamp (:start-date job))
    :status          (:status job)
    :username        (:username job)
    :deleted         (:deleted job)
    :notify          (:notify job false)
    :wiki_url        (:app-wiki-url job)
    :app_disabled    (app-disabled? app-tables (:app-id job))
    :parent_id       (:parent-id job)
    :batch           (:is-batch job)
    :batch_status    (when (:is-batch job) (format-batch-status (:id job)))}))

(defn- list-jobs*
  [{:keys [username]} {:keys [limit offset sort-field sort-dir filter include-hidden]} types]
  (jp/list-jobs-of-types username limit offset sort-field sort-dir filter include-hidden types))

(defn- count-jobs
  [{:keys [username]} {:keys [filter include-hidden]} types]
  (jp/count-jobs-of-types username filter include-hidden types))

(defn list-jobs
  [apps-client user {:keys [sort-field] :as params}]
  (let [default-sort-dir (if (nil? sort-field) :desc :asc)
        search-params    (util/default-search-params params :startdate default-sort-dir)
        types            (.getJobTypes apps-client)
        jobs             (list-jobs* user search-params types)
        app-tables       (.loadAppTables apps-client (map :app-id jobs))]
    {:analyses  (map (partial format-job app-tables) jobs)
     :timestamp (str (System/currentTimeMillis))
     :total     (count-jobs user params types)}))

(defn list-job
  [apps-client job-id]
  (let [job-info (jp/get-job-by-id job-id)]
    (format-job (.loadAppTables apps-client [(:app-id job-info)]) job-info)))

(defn- format-job-step
  [step]
  (remove-nil-vals
   {:step_number     (:step-number step)
    :external_id     (:external-id step)
    :startdate       (job-timestamp (:start-date step))
    :enddate         (job-timestamp (:end-date step))
    :status          (:status step)
    :app_step_number (:app-step-number step)
    :step_type       (:job-type step)}))

(defn list-job-steps
  [job-id]
  (let [steps (jp/list-job-steps job-id)]
    {:analysis_id job-id
     :steps       (map format-job-step steps)
     :timestamp   (str (System/currentTimeMillis))
     :total       (count steps)}))
