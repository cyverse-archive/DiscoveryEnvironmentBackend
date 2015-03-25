(ns donkey.services.metadata.util
  (:use [clojure-commons.core :only [remove-nil-values]]
        [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.util.config :as config]
            [donkey.util.service :as service]
            [kameleon.db :as db]))

(defn is-completed?
  [job-status]
  (jp/completed-status-codes job-status))

(defn is-running?
  [job-status]
  (= jp/running-status job-status))

(def not-completed? (complement is-completed?))

(defn assert-agave-enabled
  [agave]
  (when-not agave
    (service/bad-request "HPC_JOBS_DISABLED")))

(defn update-submission-result-folder
  [submission result-folder-path]
  (assoc submission
    :output_dir           result-folder-path
    :create_output_subdir false))

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
  (remove-nil-values
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

(defn- send-url-import-status-notification
  [{:keys [username start-date] :as job} status end-time]
  (let [username     (string/replace username #"@.*" "")
        end-millis   (db/timestamp-str end-time)
        start-millis (db/timestamp-str start-date)]
    (dn/send-url-import-status-notification username (assoc (format-job [] job)
                                                       :status    status
                                                       :enddate   end-millis
                                                       :startdate start-millis))))

(defn- send-analysis-status-notification
  "Sends a job status change notification."
  [{:keys [username start-date parent-id] :as job} status end-time]
  (when-not parent-id
    (let [username     (string/replace username #"@.*" "")
          end-millis   (db/timestamp-str end-time)
          start-millis (db/timestamp-str start-date)
          email        (:email current-user)]
      (dn/send-job-status-update username email (assoc (format-job [] job)
                                                  :status    status
                                                  :enddate   end-millis
                                                  :startdate start-millis)))))

;; TODO: come up with a better way to associate internal apps with notification types.
(defn send-job-status-notification
  [{:keys [app-id] :as job} status end-time]
  (if (= app-id (str (config/fileio-url-import-app)))
    (send-url-import-status-notification job status end-time)
    (send-analysis-status-notification job status end-time)))
