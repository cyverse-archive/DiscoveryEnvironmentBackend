(ns metadactyl.routes.analyses
  (:use [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.analysis.listing]
        [metadactyl.routes.params]
        [compojure.api.sweet]
        [metadactyl.user :only [current-user]]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.service :as service]))

(defroutes* analyses
  (GET* "/" [:as {:keys [uri]}]
        :query   [params SecuredIncludeHiddenPagingParams]
        :return  AnalysisList
        :notes "This service allows users to list analyses that they've previously submitted
        for execution."
        (service/coerced-trap uri AnalysisList apps/list-jobs current-user params))

  (POST* "/" [:as {:keys [uri]}]
         :query  [params SecuredQueryParamsEmailRequired]
         :body   [body AnalysisSubmission]
         :return JexSubmission
         :notes  "This service allows users to submit analyses for execution. The `config`
         element in the analysis submission is a map from parameter IDs as they appear in
         the response from the `/apps/:app-id` endpoint to the desired values for those
         parameters."
         (service/coerced-trap uri JexSubmission apps/submit-job current-user body)))
