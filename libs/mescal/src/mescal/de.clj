(ns mescal.de
  (:require [mescal.agave-de-v2 :as v2]
            [mescal.core :as core]))

(defprotocol DeAgaveClient
  "An Agave client with customizations that are specific to the discovery environment."
  (hpcAppGroup [_])
  (listApps [_])
  (getApp [_ app-id])
  (getAppDetails [_ app-id])
  (getAppDeployedComponent [_ app-id])
  (submitJob [_ submission])
  (listJobs [_] [_ job-ids])
  (listJobIds [_])
  (translateJobStatus [_ status]))

(deftype DeAgaveClientV2 [agave jobs-enabled? irods-home]
  DeAgaveClient
  (hpcAppGroup [_]
    (v2/hpc-app-group))
  (listApps [_]
    (v2/list-apps agave jobs-enabled?))
  (getApp [_ app-id]
    (v2/get-app agave app-id))
  (getAppDetails [_ app-id]
    (v2/get-app-details agave app-id))
  (getAppDeployedComponent [_ app-id]
    (v2/get-app-deployed-component agave app-id))
  (submitJob [_ submission]
    (v2/submit-job agave irods-home submission))
  (listJobs [_]
    (v2/list-jobs agave irods-home jobs-enabled?))
  (listJobs [_ job-ids]
    (v2/list-jobs agave irods-home jobs-enabled? job-ids))
  (listJobIds [_]
    (mapv :id (.listJobs agave)))
  (translateJobStatus [_ status]
    (v2/translate-job-status status)))

(defn de-agave-client-v2
  [base-url token-info jobs-enabled? irods-home & {:keys [timeout] :or {timeout 5000}}]
  (DeAgaveClientV2.
   (core/agave-client-v2 base-url token-info :timeout timeout)
   jobs-enabled?
   irods-home))
