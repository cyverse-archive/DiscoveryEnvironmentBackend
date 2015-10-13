(ns metadactyl.routes.callbacks
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.callback]
        [metadactyl.routes.params]
        [ring.util.http-response :only [ok]])
  (:require [metadactyl.service.callbacks :as callbacks]))

(defroutes* callbacks
  (POST* "/de-job" []
         :body [body (describe DeJobStatusUpdate "The App to add.")]
         :summary "Update the status of of a DE analysis."
         :description "The jex-events service calls this endpoint when the status of a DE analysis
         changes"
         (ok (callbacks/update-de-job-status body)))

  (POST* "/agave-job/:job-id" []
         :path-params [job-id :- AnalysisIdPathParam]
         :query [params AgaveJobStatusUpdate]
         :summary "Update the status of an Agave analysis."
         :description "The DE registers this endpoint as a callback when it submts jobs to Agave."
         (ok (callbacks/update-agave-job-status job-id params))))
