(ns metadactyl.service.apps.de
  (:use [kameleon.uuids :only [uuidify]])
  (:require [clojure.string :as string]
            [metadactyl.clients.jex :as jex]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.de.admin :as app-admin]
            [metadactyl.service.apps.de.categorization :as app-categorization]
            [metadactyl.service.apps.de.docs :as docs]
            [metadactyl.service.apps.de.edit :as edit]
            [metadactyl.service.apps.de.jobs :as de-jobs]
            [metadactyl.service.apps.de.job-view :as job-view]
            [metadactyl.service.apps.de.listings :as listings]
            [metadactyl.service.apps.de.metadata :as app-metadata]
            [metadactyl.service.apps.de.pipeline-edit :as pipeline-edit]
            [metadactyl.service.apps.de.validation :as app-validation]
            [metadactyl.service.apps.job-listings :as job-listings]
            [metadactyl.service.apps.util :as apps-util]
            [metadactyl.service.util :as util]))

(deftype DeApps [user]
  metadactyl.protocols.Apps

  (getUser [_]
    user)

  (getClientName [_]
    jp/de-client-name)

  (getJobTypes [_]
    [jp/de-job-type])

  (listAppCategories [_ params]
    (listings/get-app-groups user params))

  (hasCategory [_ category-id]
    (listings/has-category category-id))

  (listAppsInCategory [_ category-id params]
    (listings/list-apps-in-group user category-id params))

  (searchApps [_ _ params]
    (listings/search-apps user params))

  (canEditApps [_]
    true)

  (addApp [_ app]
    (edit/add-app user app))

  (previewCommandLine [_ app]
    (app-metadata/preview-command-line app))

  (listAppIds [_]
    (listings/list-app-ids))

  (deleteApps [_ deletion-request]
    (app-metadata/delete-apps user deletion-request))

  (getAppJobView [_ app-id]
    (when (util/uuid? app-id)
      (job-view/get-app (uuidify app-id))))

  (deleteApp [_ app-id]
    (when (util/uuid? app-id)
      (app-metadata/delete-app user (uuidify app-id))))

  (relabelApp [_ app]
    (when (util/uuid? (:id app))
      (edit/relabel-app user app)))

  (updateApp [_ app]
    (when (util/uuid? (:id app))
      (edit/update-app user app)))

  (copyApp [_ app-id]
    (when (util/uuid? app-id)
      (edit/copy-app user app-id)))

  (getAppDetails [_ app-id]
    (when (util/uuid? app-id)
      (listings/get-app-details (uuidify app-id))))

  (removeAppFavorite [_ app-id]
    (when (util/uuid? app-id)
      (app-metadata/remove-app-favorite user (uuidify app-id))))

  (addAppFavorite [_ app-id]
    (when (util/uuid? app-id)
      (app-metadata/add-app-favorite user app-id)))

  (isAppPublishable [_ app-id]
    (when (util/uuid? app-id)
      (first (app-validation/app-publishable? app-id))))

  (makeAppPublic [_ app]
    (when (util/uuid? (:id app))
      (app-metadata/make-app-public user app)))

  (deleteAppRating [_ app-id]
    (when (util/uuid? app-id)
      (app-metadata/delete-app-rating user app-id)))

  (rateApp [_ app-id rating]
    (when (util/uuid? app-id)
      (app-metadata/rate-app user app-id rating)))

  (getAppTaskListing [_ app-id]
    (when (util/uuid? app-id)
      (listings/get-app-task-listing (uuidify app-id))))

  (getAppToolListing [_ app-id]
    (when (util/uuid? app-id)
      (listings/get-app-tool-listing (uuidify app-id))))

  (getAppUi [_ app-id]
    (when (util/uuid? app-id)
      (edit/get-app-ui user app-id)))

  (addPipeline [_ pipeline]
    (pipeline-edit/add-pipeline user pipeline))

  (formatPipelineTasks [_ pipeline]
    pipeline)

  (updatePipeline [_ pipeline]
    (pipeline-edit/update-pipeline user pipeline))

  (copyPipeline [_ app-id]
    (pipeline-edit/copy-pipeline user app-id))

  (editPipeline [_ app-id]
    (pipeline-edit/edit-pipeline user app-id))

  (listJobs [self params]
    (job-listings/list-jobs self user params))

  (loadAppTables [_ app-ids]
    (->> (filter util/uuid? app-ids)
         (ap/load-app-details)
         (map (juxt (comp str :id) identity))
         (into {})
         (vector)))

  (submitJob [this submission]
    (when (util/uuid? (:app_id submission))
      (de-jobs/submit user (update-in submission [:app_id] uuidify))))

  (submitJobStep [_ _ submission]
    (de-jobs/submit-step user (update-in submission [:app_id] uuidify)))

  (translateJobStatus [self job-type status]
    (when (apps-util/supports-job-type? self job-type)
      status))

  (updateJobStatus [self job-step job status end-date]
    (when (apps-util/supports-job-type? self (:job-type job-step))
      (de-jobs/update-job-status job-step job status end-date)))

  (getDefaultOutputName [_ io-map source-step]
    (de-jobs/get-default-output-name io-map source-step))

  (getJobStepStatus [_ job-step]
    (de-jobs/get-job-step-status job-step))

  (prepareStepSubmission [_ _ submission]
    (de-jobs/prepare-step user (update-in submission [:app_id] uuidify)))

  (getParamDefinitions [_ app-id]
    (when (util/uuid? app-id)
      (app-metadata/get-param-definitions app-id)))

  (stopJobStep [self {:keys [job-type external-id]}]
    (when (and (apps-util/supports-job-type? self job-type)
               (not (string/blank? external-id)))
      (jex/stop-job external-id)))

  (categorizeApps [_ body]
    (app-categorization/categorize-apps body))

  (permanentlyDeleteApps [_ body]
    (app-metadata/permanently-delete-apps user body))

  (adminDeleteApp [_ app-id]
    (app-admin/delete-app app-id))

  (adminUpdateApp [_ body]
    (app-admin/update-app body))

  (getAdminAppCategories [_ params]
    (listings/get-admin-app-groups params))

  (adminAddCategory [_ body]
    (app-admin/add-category body))

  (adminDeleteCategories [_ body]
    (app-admin/delete-categories user body))

  (adminDeleteCategory [_ category-id]
    (app-admin/delete-category user category-id))

  (adminUpdateCategory [_ body]
    (app-admin/update-category body))

  (getAppDocs [_ app-id]
    (when (util/uuid? app-id)
      (docs/get-app-docs (uuidify app-id))))

  (ownerEditAppDocs [_ app-id body]
    (when (util/uuid? app-id)
      (docs/owner-edit-app-docs user (uuidify app-id) body)))

  (ownerAddAppDocs [_ app-id body]
    (when (util/uuid? app-id)
      (docs/owner-add-app-docs user (uuidify app-id) body)))

  (adminEditAppDocs [_ app-id body]
    (when (util/uuid? app-id)
      (docs/edit-app-docs user (uuidify app-id) body)))

  (adminAddAppDocs [_ app-id body]
    (when (util/uuid? app-id)
      (docs/add-app-docs user (uuidify app-id) body))))
