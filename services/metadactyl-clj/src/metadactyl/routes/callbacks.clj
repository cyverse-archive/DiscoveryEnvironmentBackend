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
         :summary "Update the status of a of a DE analysis."
         :notes   "The jex-events service calls this endpoint when the status of a DE analysis
         changes"
         (service/trap uri callbacks/update-de-job-status body)))
