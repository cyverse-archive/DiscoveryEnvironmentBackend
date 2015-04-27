(ns metadactyl.routes.callbacks
  (:use [compojure.api.sweet]
        [metadactyl.routes.domain.callback]
        [metadactyl.routes.params]
        [ring.swagger.schema :only [describe]])
  (:require [compojure.core :as route]
            [metadactyl.service.callbacks :as callbacks]
            [metadactyl.util.service :as service]))

(defroutes* callbacks
  (POST* "/de-job" [:as {:keys [uri]}]
         :body [body (describe DeJobStatusUpdate "The App to add.")]
         :summary "Update the status of of a DE analysis."
         :notes "The jex-events service calls this endpoint when the status of a DE analysis
         changes"
         #_(service/trap uri callbacks/update-de-job-status body)
         (service/trap uri (constantly {})))

  (POST* "/agave-job/:job-id" [:as {:keys [uri]}]
         :path-params [job-id :- AnalysisIdPathParam]
         :query [params AgaveJobStatusUpdate]
         :summary "Update the status of an Agave analysis."
         :notes "The DE registers this endpoint as a callback when it submts jobs to Agave."
         (service/trap uri callbacks/update-agave-job-status job-id params)))
