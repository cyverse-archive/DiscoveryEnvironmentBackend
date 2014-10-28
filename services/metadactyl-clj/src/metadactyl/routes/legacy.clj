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

  (POST "/make-analysis-public" [:as {body :body}]
        (trap #(throw+ '("make-app-public" body))))

  (GET "/reference-genomes" []
       (throw+ '("list-reference-genomes")))

  (PUT "/reference-genomes" [:as {body :body}]
       (throw+ '("replace-reference-genomes" (slurp body))))

  (route/not-found (unrecognized-path-response)))

(defroutes* metadactyl-routes
  (POST "/delete-categories" [:as {body :body}]
        (trap #(throw+ '("delete-categories" body))))

  (GET "/validate-analysis-for-pipelines/:app-id" [app-id]
       (trap #(throw+ '("validate-app-for-pipelines" app-id))))

  (GET "/get-analysis-categories/:category-set" [category-set]
       (trap #(throw+ '("get-app-categories" category-set))))

  (POST "/add-analysis-to-group" [:as {body :body}]
        (trap #(throw+ '("add-app-to-group" body))))

  (GET "/export-workflow/:app-id" [app-id]
       (trap #(throw+ '("export-workflow" app-id))))

  (POST "/preview-template" [:as {body :body}]
        (trap #(throw+ '("preview-template" body))))

  (POST "/preview-workflow" [:as {body :body}]
        (trap #(throw+ '("preview-workflow" body))))

  (route/not-found (unrecognized-path-response)))
