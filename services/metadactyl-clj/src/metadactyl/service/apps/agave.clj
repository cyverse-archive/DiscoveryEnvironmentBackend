(ns metadactyl.service.apps.agave
  (:use [kameleon.uuids :only [uuidify]])
  (:require [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.agave.listings :as listings]
            [metadactyl.service.apps.agave.pipelines :as pipelines]
            [metadactyl.service.apps.agave.jobs :as agave-jobs]
            [metadactyl.service.apps.job-listings :as job-listings]
            [metadactyl.service.util :as util]))

(deftype AgaveApps [agave user-has-access-token? user]
  metadactyl.protocols.Apps

  (getUser [_]
    user)

  (getClientName [_]
    jp/agave-client-name)

  (getJobTypes [_]
    [jp/agave-job-type])

  (listAppCategories [_ {:keys [hpc]}]
    (when-not (and hpc (.equalsIgnoreCase hpc "false"))
      [(.hpcAppGroup agave)]))

  (hasCategory [_ category-id]
    (= category-id (uuidify (:id (.hpcAppGroup agave)))))

  (listAppsInCategory [_ category-id params]
    (when (= category-id (uuidify (:id (.hpcAppGroup agave))))
      (listings/list-apps agave category-id params)))

  (searchApps [_ search-term params]
    (when (user-has-access-token?)
      (listings/search-apps agave search-term params)))

  (canEditApps [_]
    false)

  (listAppIds [_]
    nil)

  (getAppJobView [_ app-id]
    (when-not (util/uuid? app-id)
      (.getApp agave app-id)))

  (getAppDescription [_ app-id]
    (when-not (util/uuid? app-id)
      (:description (.getApp agave app-id))))

  (getAppDetails [_ app-id]
    (when-not (util/uuid? app-id)
      (.getAppDetails agave app-id)))

  (isAppPublishable [_ app-id]
    (when-not (util/uuid? app-id)
      false))

  (getAppTaskListing [_ app-id]
    (when-not (util/uuid? app-id)
      (.listAppTasks agave app-id)))

  (getAppToolListing [_ app-id]
    (when-not (util/uuid? app-id)
      (.getAppToolListing agave app-id)))

  (formatPipelineTasks [_ pipeline]
    (pipelines/format-pipeline-tasks agave pipeline))

  (listJobs [self params]
    (job-listings/list-jobs self user params))

  (loadAppTables [_ _]
    (if (user-has-access-token?)
      (->> (.listApps agave)
           (:apps)
           (map (juxt :id identity))
           (into {})
           (vector))
      []))

  (submitJob [this submission]
    (when-not (util/uuid? (:app_id submission))
      (agave-jobs/submit agave user submission)))

  (submitJobStep [_ job-id submission]
    (agave-jobs/submit-step agave job-id submission))

  (translateJobStatus [self job-type status]
    (when (contains? (set (.getJobTypes self)) job-type)
      (or (.translateJobStatus agave status) status)))

  (updateJobStatus [self job-step job status end-date]
    (when (contains? (set (.getJobTypes self)) (:job-type job-step))
      (agave-jobs/update-job-status agave job-step job status end-date)))

  (getDefaultOutputName [_ io-map source-step]
    (agave-jobs/get-default-output-name agave io-map source-step))

  (getJobStepStatus [_ job-step]
    (agave-jobs/get-job-step-status agave job-step))

  (prepareStepSubmission [_ job-id submission]
    (agave-jobs/prepare-step-submission agave job-id submission))

  (getParamDefinitions [_ app-id]
    (listings/get-param-definitions agave app-id)))
