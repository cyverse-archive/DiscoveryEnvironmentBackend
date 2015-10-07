(ns metadactyl.routes.analyses
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.analysis.listing]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.coercions :only [coerce!]]
        [ring.util.http-response :only [ok]])
  (:require [metadactyl.json :as json]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.coercions :as coercions]))

(defroutes* analyses
  (GET* "/" []
        :query   [{:keys [filter] :as params} SecuredAnalysisListingParams]
        :return  AnalysisList
        :summary "List Analyses"
        :description "This service allows users to list analyses that they've previously submitted
        for execution."
        ;; JSON query params are not currently supported by compojure-api,
        ;; so we have to decode the String filter param and validate it here.
        (ok (coerce! AnalysisList
                 (apps/list-jobs current-user
                   (coercions/coerce!
                     (assoc SecuredAnalysisListingParams OptionalKeyFilter [FilterParams])
                     (assoc params :filter (json/from-json filter)))))))

  (POST* "/" []
         :query   [params SecuredQueryParamsEmailRequired]
         :body    [body AnalysisSubmission]
         :return  AnalysisResponse
         :summary "Submit an Analysis"
         :description   "This service allows users to submit analyses for execution. The `config`
         element in the analysis submission is a map from parameter IDs as they appear in
         the response from the `/apps/:app-id` endpoint to the desired values for those
         parameters."
         (ok (coerce! AnalysisResponse
                  (apps/submit-job current-user body))))

  (PATCH* "/:analysis-id" []
          :path-params [analysis-id :- AnalysisIdPathParam]
          :query       [params SecuredQueryParams]
          :body        [body AnalysisUpdate]
          :return      AnalysisUpdateResponse
          :summary     "Update an Analysis"
          :description       "This service allows an analysis name or description to be updated."
          (ok (coerce! AnalysisUpdateResponse
                   (apps/update-job current-user analysis-id body))))

  (DELETE* "/:analysis-id" []
           :path-params [analysis-id :- AnalysisIdPathParam]
           :query       [params SecuredQueryParams]
           :summary     "Delete an Analysis"
           :description       "This service marks an analysis as deleted in the DE database."
           (ok (apps/delete-job current-user analysis-id)))

  (POST* "/shredder" []
         :query   [params SecuredQueryParams]
         :body    [body AnalysisShredderRequest]
         :summary "Delete Multiple Analyses"
         :description   "This service allows the caller to mark one or more analyses as deleted
         in the apps database."
         (ok (apps/delete-jobs current-user body)))

  (GET* "/:analysis-id/parameters" []
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AnalysisParameters
        :summary     "Display the parameters used in an analysis."
        :description       "This service returns a list of parameter values used in a previously
        executed analysis."
        (ok (coerce! AnalysisParameters
                 (apps/get-parameter-values current-user analysis-id))))

  (GET* "/:analysis-id/relaunch-info" []
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AppJobView
        :summary     "Obtain information to relaunch analysis."
        :description       "This service allows the Discovery Environment user interface to obtain an
        app description that can be used to relaunch a previously submitted job, possibly with
        modified parameter values."
        (ok (coerce! AppJobView
                 (apps/get-job-relaunch-info current-user analysis-id))))

  (POST* "/:analysis-id/stop" []
         :path-params [analysis-id :- AnalysisIdPathParam]
         :query       [params SecuredQueryParams]
         :return      StopAnalysisResponse
         :summary     "Stop a running analysis."
         :description       "This service allows DE users to stop running analyses."
         (ok (coerce! StopAnalysisResponse
                  (apps/stop-job current-user analysis-id))))

  (GET* "/:analysis-id/steps" []
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AnalysisStepList
        :summary     "Display the steps of an analysis."
        :description "This service returns a list of steps in an analysis."
        (ok (apps/list-job-steps current-user analysis-id))))
