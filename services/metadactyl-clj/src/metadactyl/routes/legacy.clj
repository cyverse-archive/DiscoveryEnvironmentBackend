(ns metadactyl.routes.legacy
  (:use [metadactyl.app-categorization]
        [metadactyl.app-listings]
        [metadactyl.metadata.tool-requests]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.service]
        [compojure.api.sweet]
        [compojure.core :only [GET PUT POST]]
        [slingshot.slingshot :only [throw+]])
  (:require [compojure.route :as route]
            [clojure-commons.error-codes :as ce]
            [metadactyl.service.app-metadata :as app-metadata]
            [metadactyl.util.config :as config]))

(defroutes* secured-routes
  (GET "/bootstrap" [:as {params :params headers :headers}]
       (throw+ "(bootstrap (:ip-address params) (headers \"user-agent\"))"))

  (GET "/logout" [:as {params :params}]
       (ce/trap "logout" #(throw+ '("logout" params))))

  (GET "/template/:app-id" [app-id]
       (throw+ '("get-app" app-id)))

  (GET "/app/:app-id" [app-id]
       (ce/trap "app" #(throw+ '("get-app-new-format" app-id))))

  (PUT "/workspaces/:workspace-id/newexperiment" [workspace-id :as {body :body}]
       (throw+ '("run-experiment" body workspace-id)))

  (POST "/rate-analysis" [:as {body :body}]
        (throw+ '("rate-app" body)))

  (POST "/delete-rating" [:as {body :body}]
        (throw+ '("delete-rating" body)))

  (GET "/get-components-in-analysis/:app-id" [app-id]
       (throw+ '("list-deployed-components-in-app" app-id)))

  (POST "/update-favorites" [:as {body :body}]
        (throw+ '("update-favorites" body)))

  (GET "/edit-template/:app-id" [app-id]
       (throw+ '("edit-app" app-id)))

  (GET "/edit-app/:app-id" [app-id]
       (throw+ '("edit-app-new-format" app-id)))

  (GET "/copy-template/:app-id" [app-id]
       (throw+ '("copy-app" app-id)))

  (PUT "/update-template" [:as {body :body}]
       (trap #(throw+ '("update-template-secured" body))))

  (PUT "/update-app" [:as {body :body}]
       (ce/trap "update-app" #(throw+ '("update-app-secured" body))))

  (POST "/update-workflow" [:as {body :body}]
        (trap #(throw+ '("update-workflow" body))))

  (POST "/make-analysis-public" [:as {body :body}]
        (trap #(throw+ '("make-app-public" body))))

  (GET "/reference-genomes" []
       (throw+ '("list-reference-genomes")))

  (PUT "/reference-genomes" [:as {body :body}]
       (throw+ '("replace-reference-genomes" (slurp body))))

  (route/not-found (unrecognized-path-response)))

(defroutes* metadactyl-routes
  (GET "/" []
       "Welcome to Metadactyl!\n")

  (GET "/search-deployed-components/:search-term" [search-term]
       (trap #(throw+ '("search-deployed-components" search-term))))

  (POST "/delete-categories" [:as {body :body}]
        (trap #(throw+ '("delete-categories" body))))

  (GET "/validate-analysis-for-pipelines/:app-id" [app-id]
       (trap #(throw+ '("validate-app-for-pipelines" app-id))))

  (GET "/apps/:app-id/data-objects" [app-id]
       (trap #(throw+ '("get-data-objects-for-app" app-id))))

  (GET "/get-analysis-categories/:category-set" [category-set]
       (trap #(throw+ '("get-app-categories" category-set))))

  (POST "/can-export-analysis" [:as {body :body}]
        (trap #(throw+ '("can-export-app" body))))

  (POST "/add-analysis-to-group" [:as {body :body}]
        (trap #(throw+ '("add-app-to-group" body))))

  (GET "/get-analysis/:app-id" [app-id]
       (trap #(throw+ '("get-app" app-id))))

  (GET "/list-analysis/:app-id" [app-id]
       (throw+ '("list-app" app-id)))

  (GET "/export-template/:template-id" [template-id]
       (trap #(throw+ '("export-template" template-id))))

  (GET "/export-workflow/:app-id" [app-id]
       (trap #(throw+ '("export-workflow" app-id))))

  (POST "/export-deployed-components" [:as {body :body}]
        (trap #(throw+ '("export-deployed-components" body))))

  (POST "/preview-template" [:as {body :body}]
        (trap #(throw+ '("preview-template" body))))

  (POST "/preview-workflow" [:as {body :body}]
        (trap #(throw+ '("preview-workflow" body))))

  (POST "/update-template" [:as {body :body}]
        (trap #(throw+ '("update-template" body))))

  (POST "/force-update-workflow" [:as {body :body params :params}]
        (trap #(throw+ '("force-update-workflow" body params))))

  (POST "/update-workflow" [:as {body :body}]
        (trap #(throw+ '("update-workflow" body))))

  (POST "/import-template" [:as {body :body}]
        (trap #(throw+ '("import-template" body))))

  (POST "/import-workflow" [:as {body :body}]
        (trap #(throw+ '("import-workflow" body))))

  (POST "/import-tools" [:as {body :body}]
        (trap #(throw+ '("import-tools" body))))

  (POST "/update-analysis" [:as {body :body}]
        (trap #(throw+ '("update-app" body))))

  (POST "/update-app-labels" [:as {body :body}]
        (ce/trap "update-app-labels" #(app-metadata/relabel-app body)))

  (POST "/arg-preview" [:as {body :body}]
        (ce/trap "arg-preview" #(app-metadata/preview-command-line body)))

  (route/not-found (unrecognized-path-response)))

