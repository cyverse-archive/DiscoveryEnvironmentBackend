(ns mescal.de
  (:require [mescal.agave-de-v2 :as v2]
            [mescal.core :as core]))

(defprotocol DeAgaveClient
  "An Agave client with customizations that are specific to the discovery environment."
  (hpcAppGroup [_])
  (listApps [_])
  (searchApps [_ search-term])
  (getApp [_ app-id])
  (getAppDetails [_ app-id])
  (listAppTasks [_ app-id])
  (getAppToolListing [_ app-id])
  (submitJob [_ submission])
  (prepareJobSubmission [_ submission])
  (sendJobSubmission [_ submission])
  (listJobs [_] [_ job-ids])
  (listJob [_ job-id])
  (listJobIds [_])
  (stopJob [_ job-id])
  (getJobParams [_ job-id])
  (getAppRerunInfo [_ job-id])
  (translateJobStatus [_ status])
  (regenerateJobSubmission [_ job-id])
  (getDefaultOutputName [_ app-id output-id]))

(deftype DeAgaveClientV2 [agave jobs-enabled?]
  DeAgaveClient
  (hpcAppGroup [_]
    (v2/hpc-app-group))
  (listApps [_]
    (v2/list-apps agave jobs-enabled?))
  (searchApps [_ search-term]
    (v2/search-apps agave jobs-enabled? search-term))
  (getApp [_ app-id]
    (v2/get-app agave app-id))
  (getAppDetails [_ app-id]
    (v2/get-app-details agave app-id))
  (listAppTasks [_ app-id]
    (v2/list-app-tasks agave app-id))
  (getAppToolListing [_ app-id]
    (v2/get-app-tool-listing agave app-id))
  (submitJob [this submission]
    (->> (.prepareJobSubmission this submission)
         (.sendJobSubmission this)))
  (prepareJobSubmission [_ submission]
    (v2/prepare-job-submission agave submission))
  (sendJobSubmission [_ submission]
    (v2/send-job-submission agave submission))
  (listJobs [_]
    (v2/list-jobs agave jobs-enabled?))
  (listJobs [_ job-ids]
    (v2/list-jobs agave jobs-enabled? job-ids))
  (listJob [_ job-id]
    (v2/list-job agave jobs-enabled? job-id))
  (listJobIds [_]
    (mapv :id (.listJobs agave)))
  (stopJob [_ job-id]
    (.stopJob agave job-id))
  (getJobParams [_ job-id]
    (v2/get-job-params agave job-id))
  (getAppRerunInfo [_ job-id]
    (v2/get-app-rerun-info agave job-id))
  (translateJobStatus [_ status]
    (v2/translate-job-status status))
  (regenerateJobSubmission [_ job-id]
    (v2/regenerate-job-submission agave job-id))
  (getDefaultOutputName [_ app-id output-id]
    (v2/get-default-output-name agave app-id output-id)))

(defn de-agave-client-v2
  [base-url storage-system token-info-fn jobs-enabled? & {:keys [timeout] :or {timeout 5000}}]
  (DeAgaveClientV2.
   (core/agave-client-v2 base-url storage-system token-info-fn :timeout timeout)
   jobs-enabled?))
