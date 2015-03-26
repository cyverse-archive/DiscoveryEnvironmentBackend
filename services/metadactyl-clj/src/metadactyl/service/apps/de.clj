(ns metadactyl.service.apps.de
  (:use [kameleon.uuids :only [uuidify]])
  (:require [metadactyl.service.apps.de.edit :as edit]
            [metadactyl.service.apps.de.job-view :as job-view]
            [metadactyl.service.apps.de.listings :as listings]
            [metadactyl.service.apps.de.metadata :as app-metadata]
            [metadactyl.service.apps.de.pipeline-edit :as pipeline-edit]
            [metadactyl.service.apps.de.validation :as app-validation]
            [metadactyl.service.util :as util]))

(deftype DeApps [user]
  metadactyl.protocols.Apps

  (getClientName [_]
    "de")

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

  (getAppDescription [_ app-id]
    (when (util/uuid? app-id)
      (listings/get-app-description (uuidify app-id))))

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
    (pipeline-edit/update-pipeline user pipeline)))
