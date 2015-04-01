(ns metadactyl.service.apps.job-listings
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
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
  [apps-client user params]
  (let [types      (.getJobTypes apps-client)
        jobs       (list-jobs* user (util/default-search-params params :startdate :desc) types)
        app-tables (.loadAppTables apps-client (map :app-id jobs))]
    {:analyses  (map (partial format-job app-tables) jobs)
     :timestamp (str (System/currentTimeMillis))
     :total     (count-jobs user params types)}))
