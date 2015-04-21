(ns metadactyl.routes.analyses
  (:use [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.analysis.listing]
        [metadactyl.routes.params]
        [compojure.api.sweet]
        [metadactyl.user :only [current-user]]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.json :as json]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.coercions :as coercions]
            [metadactyl.util.service :as service]))

(defroutes* analyses
  (GET* "/" [:as {:keys [uri]}]
        :query   [{:keys [filter] :as params} SecuredAnalysisListingParams]
        :return  AnalysisList
        :notes "This service allows users to list analyses that they've previously submitted
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
         :query  [params SecuredQueryParamsEmailRequired]
         :body   [body AnalysisSubmission]
         :return AnalysisResponse
         :notes  "This service allows users to submit analyses for execution. The `config`
         element in the analysis submission is a map from parameter IDs as they appear in
         the response from the `/apps/:app-id` endpoint to the desired values for those
         parameters."
         (service/coerced-trap uri AnalysisResponse apps/submit-job current-user body)))
