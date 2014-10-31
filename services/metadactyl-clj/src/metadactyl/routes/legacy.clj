(ns metadactyl.routes.legacy
  (:use [metadactyl.app-categorization]
        [metadactyl.app-listings]
        [metadactyl.metadata.tool-requests]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.service]
        [compojure.api.sweet]
        [compojure.api.legacy]
        [slingshot.slingshot :only [throw+]])
  (:require [compojure.route :as route]
            [clojure-commons.error-codes :as ce]
            [metadactyl.util.config :as config]))

(defroutes* secured-routes
  (PUT "/workspaces/:workspace-id/newexperiment" [workspace-id :as {body :body}]
       (throw+ '("run-experiment" body workspace-id)))

  (route/not-found (unrecognized-path-response)))

(defroutes* metadactyl-routes
  (GET "/export-workflow/:app-id" [app-id]
       (trap #(throw+ '("export-workflow" app-id))))

  (route/not-found (unrecognized-path-response)))
