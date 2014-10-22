(ns metadactyl.routes.analyses
  (:use [metadactyl.routes.domain.analysis]
        [metadactyl.routes.params]
        [compojure.api.sweet]
        [metadactyl.util.service :only [success-response]]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.analyses :as analyses]))

(defroutes* analyses
  (POST* "/" [:as {:keys [uri]}]
         :query  [params SecuredQueryParamsEmailRequired]
         :body   [body AnalysisSubmission]
         :return JexSubmission
         :notes  "This service allows users to submit analyses for execution. The `config`
         element in the analysis submission is a map from parameter IDs as they appear in
         the response from the `/apps/:app-id` endpoint to the desired values for those
         parameters."
         (ce/trap uri #(success-response (analyses/submit params body)))))
