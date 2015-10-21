(ns metadactyl.service.apps.jobs
  (:use [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [kameleon.db :as db]
            [metadactyl.clients.notifications :as cn]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.job-listings :as listings]
            [metadactyl.service.apps.jobs.params :as job-params]
            [metadactyl.service.apps.jobs.submissions :as submissions]
            [metadactyl.util.service :as service]))

(defn supports-job-type
  [apps-client job-type]
  (contains? (set (.getJobTypes apps-client)) job-type))

(defn get-unique-job-step
  "Gets a unique job step for an external ID. An exception is thrown if no job step
  is found or if multiple job steps are found."
  [external-id]
  (let [job-steps (jp/get-job-steps-by-external-id external-id)]
    (when (empty? job-steps)
      (service/not-found "job step" external-id))
    (when (> (count job-steps) 1)
      (service/not-unique "job step" external-id))
    (first job-steps)))

(defn lock-job-step
  [job-id external-id]
  (service/assert-found (jp/lock-job-step job-id external-id) "job step"
                        (str job-id "/" external-id)))

(defn lock-job
  [job-id]
  (service/assert-found (jp/lock-job job-id) "job" job-id))

(defn- send-job-status-update
  [apps-client {job-id :id prev-status :status app-id :app-id}]
  (let [{curr-status :status :as job} (jp/get-job-by-id job-id)]
    (when-not (= prev-status curr-status)
      (cn/send-job-status-update
       (.getUser apps-client)
       (listings/format-job (.loadAppTables apps-client [app-id]) job)))))

(defn- determine-batch-status
  [{:keys [id]}]
  (let [children (jp/list-child-jobs id)]
    (cond (every? (comp jp/completed? :status) children) jp/completed-status
          (some (comp jp/running? :status) children)     jp/running-status
          :else                                          jp/submitted-status)))

(defn- update-batch-status
  [batch end-date]
  (let [new-status (determine-batch-status batch)]
    (when-not (= (:status batch) new-status)
      (jp/update-job (:id batch) {:status new-status :end-date end-date})
      (jp/update-job-steps (:id batch) new-status end-date))))

(defn update-job-status
  [apps-client job-step {:keys [id] :as job} batch status end-date]
  (when (jp/completed? (:status job))
    (service/bad-request (str "received a job status update for completed or canceled job, " id)))
  (let [end-date (db/timestamp-from-str end-date)]
    (.updateJobStatus apps-client job-step job status end-date)
    (when batch (update-batch-status batch end-date))
    (send-job-status-update apps-client (or batch job))))

(defn- find-incomplete-job-steps
  [job-id]
  (remove (comp jp/completed? :status) (jp/list-job-steps job-id)))

(defn- sync-incomplete-job-status
  [apps-client {:keys [id] :as job} step]
  (if-let [step-status (.getJobStepStatus apps-client step)]
    (let [step     (lock-job-step id (:external-id step))
          job      (lock-job id)
          batch    (when-let [parent-id (:parent-id job)] (lock-job parent-id))
          status   (:status step-status)
          end-date (:enddate step-status)]
      (update-job-status apps-client step job batch status end-date))
    (let [step  (lock-job-step id (:external-id step))
          job   (lock-job id)
          batch (when-let [parent-id (:parent-id job)] (lock-job parent-id))]
      (update-job-status apps-client step job batch jp/failed-status (db/now-str)))))

(defn- determine-job-status
  "Determines the status of a job for synchronization in the case when all job steps are
   marked as being in one of the completed statuses but the job itself is not."
  [job-id]
  (let [statuses (map :status (jp/list-job-steps job-id))
        status   (first (filter (partial not= jp/completed-status) statuses))]
    (cond (nil? status)                 jp/completed-status
          (= jp/canceled-status status) status
          (= jp/failed-status status)   status
          :else                         jp/failed-status)))

(defn- sync-complete-job-status
  [{:keys [id]}]
  (let [{:keys [status]} (jp/lock-job id)]
    (when-not (jp/completed? status)
      (jp/update-job id {:status (determine-job-status id) :end-date (db/now)}))))

(defn sync-job-status
  [apps-client {:keys [id] :as job}]
  (if-let [step (first (find-incomplete-job-steps id))]
    (sync-incomplete-job-status apps-client job step)
    (sync-complete-job-status job)))

(defn- validate-job-existence
  [job-ids]
  (let [missing-ids (jp/list-non-existent-job-ids (set job-ids))]
    (when-not (empty? missing-ids)
      (service/not-found "jobs" job-ids))))

(defn validate-job-ownership
  [username job-ids]
  (let [unowned-ids (map :id (jp/list-unowned-jobs username job-ids))]
    (when-not (empty? unowned-ids)
      (service/not-owner "jobs" (string/join ", " unowned-ids)))))

(defn- validate-jobs-for-user
  [username job-ids]
  (validate-job-existence job-ids)
  (validate-job-ownership username job-ids))

(defn update-job
  [{:keys [username]} job-id body]
  (validate-jobs-for-user username [job-id])
  (jp/update-job job-id body)
  (->> (jp/get-job-by-id job-id)
       ((juxt :id :job-name :description))
       (zipmap [:id :name :description])))

(defn delete-job
  [{:keys [username]} job-id]
  (validate-jobs-for-user username [job-id])
  (jp/delete-jobs [job-id]))

(defn delete-jobs
  [{:keys [username]} job-ids]
  (validate-jobs-for-user username job-ids)
  (jp/delete-jobs job-ids))

(defn get-parameter-values
  [apps-client {:keys [username]} job-id]
  (validate-jobs-for-user username [job-id])
  (let [job (jp/get-job-by-id job-id)]
    {:app_id     (:app-id job)
     :parameters (job-params/get-parameter-values apps-client job)}))

(defn get-job-relaunch-info
  [apps-client {:keys [username]} job-id]
  (validate-jobs-for-user username [job-id])
  (job-params/get-job-relaunch-info apps-client (jp/get-job-by-id job-id)))

(defn- stop-job-steps
  "Stops an individual step in a job."
  [apps-client {:keys [id] :as job} steps]
  (.stopJobStep apps-client (first steps))
  (jp/cancel-job-step-numbers id (mapv :step-number steps))
  (send-job-status-update apps-client job))

(defn stop-job
  [apps-client {:keys [username] :as user} job-id]
  (validate-jobs-for-user username [job-id])
  (let [{:keys [status] :as job} (jp/get-job-by-id job-id)]
    (when (listings/is-completed? status)
      (service/bad-request (str "job, " job-id ", is already completed or canceled")))
    (jp/update-job job-id jp/canceled-status (db/now))
    (try+
     (stop-job-steps apps-client job (find-incomplete-job-steps job-id))
     (catch Throwable t
       (log/warn t "unable to cancel the most recent step of job, " job-id))
     (catch Object _
       (log/warn "unable to cancel the most recent step of job, " job-id)))))

(defn list-job-steps
  [{:keys [username]} job-id]
  (validate-jobs-for-user username [job-id])
  (listings/list-job-steps job-id))

(defn submit
  [apps-client user submission]
  (transaction (submissions/submit apps-client user submission)))
