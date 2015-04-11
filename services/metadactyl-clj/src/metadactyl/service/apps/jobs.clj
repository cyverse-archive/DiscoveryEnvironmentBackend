(ns metadactyl.service.apps.jobs
  (:require [kameleon.db :as db]
            [metadactyl.persistence.jobs :as jp]
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
  (service/assert-found (jp/lock-job job-id) "job" job-id))

(defn update-job-status
  [apps-client job-step job batch status end-date]
  (.updateJobStatus apps-client job-step job batch status (db/timestamp-from-str end-date)))
