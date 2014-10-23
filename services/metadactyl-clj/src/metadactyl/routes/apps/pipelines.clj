(ns metadactyl.routes.apps.pipelines
  (:use [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.params]
        [metadactyl.zoidberg.pipeline-edit :only [add-pipeline
                                                  copy-pipeline
                                                  edit-pipeline
                                                  update-pipeline]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* pipelines
  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParamsEmailRequired]
         :body [body (describe PipelineCreateRequest "The Pipeline to create.")]
         :return Pipeline
         :summary "Create a Pipeline"
         :notes "This service adds a new Pipeline."
         (ce/trap uri #(add-pipeline body)))

  (PUT* "/:app-id" [:as {uri :uri}]
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :body [body (describe PipelineUpdateRequest "The Pipeline to update.")]
        :return Pipeline
        :summary "Update a Pipeline"
        :notes "This service updates an existing Pipeline in the database, as long as the Pipeline
        has not been submitted for public use."
        (ce/trap uri #(update-pipeline (assoc body :id app-id))))

  (POST* "/:app-id/copy" [:as {uri :uri}]
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParamsEmailRequired]
         :return Pipeline
         :summary "Make a Copy of a Pipeline Available for Editing"
         :notes "This service can be used to make a copy of a Pipeline in the user's workspace. This
         endpoint will copy the App details, steps, and mappings, but will not copy tasks used in
         the Pipeline steps."
         (ce/trap uri #(copy-pipeline app-id)))

  (GET* "/:app-id/ui" [:as {uri :uri}]
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParamsEmailRequired]
        :return Pipeline
        :summary "Make a Pipeline Available for Editing"
        :notes "The DE uses this service to obtain a JSON representation of a Pipeline for editing.
        The Pipeline must have been integrated by the requesting user, and it must not already be
        public."
        (ce/trap uri #(edit-pipeline app-id))))
