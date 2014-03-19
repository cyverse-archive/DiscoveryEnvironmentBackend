(ns mescal.de
  (:require [mescal.agave-de-v1 :as v1]
            [mescal.core :as core]))

(defprotocol DeAgaveClient
  "An Agave client with customizations that are specific to the discovery environment."
  (publicAppGroup [this])
  (listPublicApps [this params])
  (searchPublicApps [this search-term])
  (getApp [this app-id])
  (getAppDeployedComponent [this app-id])
  (getAppDetails [this app-id])
  (submitJob [this submission])
  (listJobs [this] [this job-ids])
  (listRawJob [this job-id])
  (listJobIds [this])
  (getJobParams [this job-id])
  (getAppRerunInfo [this job-id])
  (translateJobStatus [this status]))

(deftype DeAgaveClientV1 [agave jobs-enabled? irods-home]
  DeAgaveClient
  (publicAppGroup [this]
    (v1/public-app-group))
  (listPublicApps [this params]
    (v1/list-public-apps agave jobs-enabled? params))
  (searchPublicApps [this search-term]
    (v1/search-public-apps agave jobs-enabled? search-term))
  (getApp [this app-id]
    (v1/get-app agave irods-home app-id))
  (getAppDeployedComponent [this app-id]
    (v1/get-deployed-component-for-app agave app-id))
  (getAppDetails [this app-id]
    (v1/get-app-details agave app-id))
  (submitJob [this submission]
    (v1/submit-job agave irods-home submission))
  (listJobs [this]
    (v1/list-jobs agave jobs-enabled? irods-home))
  (listJobs [this job-ids]
    (v1/list-jobs agave jobs-enabled? irods-home job-ids))
  (listRawJob [this job-id]
    (v1/list-raw-job agave jobs-enabled? irods-home job-id))
  (listJobIds [this]
    (v1/list-job-ids agave))
  (getJobParams [this job-id]
    (v1/get-job-params agave irods-home job-id))
  (getAppRerunInfo [this job-id]
    (v1/get-app-rerun-info agave irods-home job-id))
  (translateJobStatus [this status]
    (v1/translate-job-status status)))

(defn de-agave-client-v1
  [base-url proxy-user proxy-pass user jobs-enabled? irods-home]
  (DeAgaveClientV1.
   (core/agave-client-v1 base-url proxy-user proxy-pass user)
   jobs-enabled?
   irods-home))
