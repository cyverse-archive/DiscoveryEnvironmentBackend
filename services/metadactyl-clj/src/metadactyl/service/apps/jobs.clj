(ns metadactyl.service.apps.jobs
  (:use [korma.db :only [transaction]])
  (:require [kameleon.db :as db]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.util.service :as service]))

(defn- get-unique-job-step
  "Gets a unique job step for an external ID. An exception is thrown if no job step
   is found or if multiple job steps are found."
  [external-id]
  (let [job-steps (jp/get-job-steps-by-external-id external-id)]
    (when (empty? job-steps)
      (service/not-found "job step" external-id))
    (when (> (count job-steps) 1)
      (service/not-unique "job step" external-id))
    (first job-steps)))

(defn- lock-job-step
  [job-id external-id]
  (service/assert-found (jp/lock-job-step job-id external-id) "job step"
                        (str job-id "/" external-id)))

(defn- lock-job
  (service/assert-found (jp/lock-job job-id) "job" job-id))

;; TODO: implement me
(defn- update-job-status*
  [apps-client job-step job batch status end-date])

(defn update-job-status
  ([apps-client external-id status end-date]
     (let [{:keys [job-id]} (get-unique-job-step external-id)]
       (update-job-status apps-client job-id external-id status end-date)))
  ([apps-client job-id external-id status end-date]
     (transaction
      (let [job-step (lock-job-step job-id external-id)
            job      (lock-job job-id)
            batch    (when-let [parent-id (:parent-id job)] (lock-job parent-id))
            status   (.translateJobStatus apps-client (:job-type job-step) status)
            end-date (db/timestamp-from-str end-date)]
        (update-job-status* apps-client job-step job batch status end-date)))))
