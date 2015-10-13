(ns metadactyl.routes.apps.pipelines
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.params]
        [metadactyl.util.coercions :only [coerce!]]
        [ring.util.http-response :only [ok]]
        [metadactyl.user :only [current-user]])
  (:require [metadactyl.service.apps :as apps]))

(defroutes* pipelines
  (POST* "/" []
         :query [params SecuredQueryParamsRequired]
         :body [body (describe PipelineCreateRequest "The Pipeline to create.")]
         :return Pipeline
         :summary "Create a Pipeline"
         :description "This service adds a new Pipeline."
         (ok (coerce! Pipeline
                  (apps/add-pipeline current-user body))))

  (PUT* "/:app-id" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :body [body (describe PipelineUpdateRequest "The Pipeline to update.")]
        :return Pipeline
        :summary "Update a Pipeline"
        :description "This service updates an existing Pipeline in the database, as long as the Pipeline
        has not been submitted for public use."
        (ok (coerce! Pipeline
                 (apps/update-pipeline current-user (assoc body :id app-id)))))

  (POST* "/:app-id/copy" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParamsRequired]
         :return Pipeline
         :summary "Make a Copy of a Pipeline Available for Editing"
         :description "This service can be used to make a copy of a Pipeline in the user's workspace. This
         endpoint will copy the App details, steps, and mappings, but will not copy tasks used in
         the Pipeline steps."
         (ok (coerce! Pipeline
                  (apps/copy-pipeline current-user app-id))))

  (GET* "/:app-id/ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :return Pipeline
        :summary "Make a Pipeline Available for Editing"
        :description "The DE uses this service to obtain a JSON representation of a Pipeline for editing.
        The Pipeline must have been integrated by the requesting user, and it must not already be
        public."
        (ok (coerce! Pipeline
                 (apps/edit-pipeline current-user app-id)))))
