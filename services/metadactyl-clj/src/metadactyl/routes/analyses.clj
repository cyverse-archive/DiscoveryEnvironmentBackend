(ns metadactyl.routes.analyses
  (:use [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.analysis.listing]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.params]
        [compojure.api.sweet]
        [metadactyl.user :only [current-user]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.json :as json]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.coercions :as coercions]
            [metadactyl.util.service :as service]))

(defroutes* analyses
  (GET* "/" [:as {:keys [uri]}]
        :query   [{:keys [filter] :as params} SecuredAnalysisListingParams]
        :return  AnalysisList
        :summary "List Analyses"
        :description "This service allows users to list analyses that they've previously submitted
        for execution."
        ;; JSON query params are not currently supported by compojure-api,
        ;; so we have to decode the String filter param and validate it here.
        (service/coerced-trap uri AnalysisList
          apps/list-jobs
          current-user
          (coercions/coerce!
            (assoc SecuredAnalysisListingParams OptionalKeyFilter [FilterParams])
            (assoc params :filter (json/from-json filter)))))

  (POST* "/" [:as {:keys [uri]}]
         :query   [params SecuredQueryParamsEmailRequired]
         :body    [body AnalysisSubmission]
         :return  AnalysisResponse
         :summary "Submit an Analysis"
         :description   "This service allows users to submit analyses for execution. The `config`
         element in the analysis submission is a map from parameter IDs as they appear in
         the response from the `/apps/:app-id` endpoint to the desired values for those
         parameters."
         (service/coerced-trap uri AnalysisResponse apps/submit-job current-user body))

  (PATCH* "/:analysis-id" [:as {:keys [uri]}]
          :path-params [analysis-id :- AnalysisIdPathParam]
          :query       [params SecuredQueryParams]
          :body        [body AnalysisUpdate]
          :return      AnalysisUpdateResponse
          :summary     "Update an Analysis"
          :description       "This service allows an analysis name or description to be updated."
          (service/coerced-trap uri AnalysisUpdateResponse
                                apps/update-job current-user analysis-id body))

  (DELETE* "/:analysis-id" [:as {:keys [uri]}]
           :path-params [analysis-id :- AnalysisIdPathParam]
           :query       [params SecuredQueryParams]
           :summary     "Delete an Analysis"
           :description       "This service marks an analysis as deleted in the DE database."
           (service/trap uri apps/delete-job current-user analysis-id))

  (POST* "/shredder" [:as {:keys [uri]}]
         :query   [params SecuredQueryParams]
         :body    [body AnalysisShredderRequest]
         :summary "Delete Multiple Analyses"
         :description   "This service allows the caller to mark one or more analyses as deleted
         in the apps database."
         (service/trap uri apps/delete-jobs current-user body))

  (GET* "/:analysis-id/parameters" [:as {:keys [uri]}]
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AnalysisParameters
        :summary     "Display the parameters used in an analysis."
        :description       "This service returns a list of parameter values used in a previously
        executed analysis."
        (service/coerced-trap uri AnalysisParameters
                              apps/get-parameter-values current-user analysis-id))

  (GET* "/:analysis-id/relaunch-info" [:as {:keys [uri]}]
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AppJobView
        :summary     "Obtain information to relaunch analysis."
        :description       "This service allows the Discovery Environment user interface to obtain an
        app description that can be used to relaunch a previously submitted job, possibly with
        modified parameter values."
        (service/coerced-trap uri AppJobView
                              apps/get-job-relaunch-info current-user analysis-id))

  (POST* "/:analysis-id/stop" [:as {:keys [uri]}]
         :path-params [analysis-id :- AnalysisIdPathParam]
         :query       [params SecuredQueryParams]
         :return      StopAnalysisResponse
         :summary     "Stop a running analysis."
         :description       "This service allows DE users to stop running analyses."
         (service/coerced-trap uri StopAnalysisResponse apps/stop-job current-user analysis-id))

  (GET* "/:analysis-id/steps" [:as {:keys [uri]}]
        :path-params [analysis-id :- AnalysisIdPathParam]
        :query       [params SecuredQueryParams]
        :return      AnalysisStepList
        :summary     "Display the steps of an analysis."
        :description "This service returns a list of steps in an analysis."
        (service/trap uri apps/list-job-steps current-user analysis-id)))
