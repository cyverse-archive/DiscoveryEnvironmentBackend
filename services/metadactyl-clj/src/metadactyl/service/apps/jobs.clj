(ns metadactyl.service.apps.jobs
  (:require [kameleon.db :as db]
            [metadactyl.clients.notifications :as cn]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.job-listings :as listings]
            [metadactyl.util.service :as service]))

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
    (service/bad-request "received a job status update for completed or canceled job, " id))
  (.updateJobStatus apps-client job-step job status (db/timestamp-from-str end-date))
  (when batch (update-batch-status batch end-date))
  (send-job-status-update apps-client (or batch job)))
