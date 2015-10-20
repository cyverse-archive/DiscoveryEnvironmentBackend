(ns metadactyl.routes.api
  (:use [service-logging.middleware :only [wrap-logging clean-context]]
        [compojure.core :only [wrap-routes]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [common-swagger-api.schema]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [service-logging.middleware :only [add-user-to-context]]
        [ring.util.response :only [redirect]])
  (:require [compojure.route :as route]
            [metadactyl.routes.admin :as admin-routes]
            [metadactyl.routes.analyses :as analysis-routes]
            [metadactyl.routes.apps :as app-routes]
            [metadactyl.routes.apps.categories :as app-category-routes]
            [metadactyl.routes.apps.elements :as app-element-routes]
            [metadactyl.routes.apps.pipelines :as pipeline-routes]
            [metadactyl.routes.callbacks :as callback-routes]
            [metadactyl.routes.collaborators :as collaborator-routes]
            [metadactyl.routes.oauth :as oauth-routes]
            [metadactyl.routes.reference-genomes :as reference-genome-routes]
            [metadactyl.routes.status :as status-routes]
            [metadactyl.routes.tools :as tool-routes]
            [metadactyl.routes.users :as user-routes]
            [metadactyl.routes.workspaces :as workspace-routes]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]
            [clojure-commons.exception :as cx]))

(defapi app
  {:exceptions cx/exception-handlers}
  (swagger-ui config/docs-uri)
  (swagger-docs
    {:info {:title "Discovery Environment Apps API"
            :description "Documentation for the Discovery Environment Apps REST API"
            :version "2.0.0"}
     :tags [{:name "service-info", :description "Service Status Information"}
            {:name "callbacks", :description "General callback functions"}
            {:name "app-categories", :description "App Category endpoints."}
            {:name "app-element-types", :description "App Element endpoints."}
            {:name "apps", :description "App endpoints."}
            {:name "pipelines", :description "Pipeline endpoints."}
            {:name "analyses", :description "Analysis endpoints."}
            {:name "tool-data-containers", :description "Tool Docker Data Container endpoints."}
            {:name "tools", :description "Tool endpoints."}
            {:name "workspaces", :description "Workspace endpoints."}
            {:name "users", :description "User endpoints."}
            {:name "tool-requests", :description "Tool Request endpoints."}
            {:name "reference-genomes", :description "Reference Genome endpoints."}
            {:name "oauth-routes", :description "OAuth callback routes."}
            {:name "collaborator-routes", :description "Collaborator Information Routes"}
            {:name "admin-apps", :description "Admin App endpoints."}
            {:name "admin-categories", :description "Admin App Category endpoints."}
            {:name "admin-container-images", :description "Admin Tool Docker Images endpoints."}
            {:name "admin-data-containers", :description "Admin Docker Data Container endpoints."}
            {:name "admin-tools", :description "Admin Tool endpoints."}
            {:name "admin-reference-genomes", :description "Admin Reference Genome endpoints."}
            {:name "admin-tool-requests", :description "Admin Tool Request endpoints."}]})
  (middlewares
    [clean-context
     wrap-keyword-params
     wrap-query-params
     (wrap-routes wrap-logging)]
    (context* "/" []
      :tags ["service-info"]
      status-routes/status)
    (context* "/callbacks" []
      :tags ["callbacks"]
      callback-routes/callbacks))
  (middlewares
    [clean-context
     wrap-keyword-params
     wrap-query-params
     add-user-to-context
     store-current-user
     wrap-logging]
    (context* "/apps/categories" []
      :tags ["app-categories"]
      app-category-routes/app-categories)
    (context* "/apps/elements" []
      :tags ["app-element-types"]
      app-element-routes/app-elements)
    (context* "/apps" []
      :tags ["apps"]
      app-routes/apps)
    (context* "/apps/pipelines" []
      :tags ["pipelines"]
      pipeline-routes/pipelines)
    (context* "/analyses" []
      :tags ["analyses"]
      analysis-routes/analyses)
    (context* "/tools/data-containers" []
      :tags ["tool-data-containers"]
      tool-routes/data-containers)
    (context* "/tools" []
      :tags ["tools"]
      tool-routes/tools)
    (context* "/workspaces" []
      :tags ["workspaces"]
      workspace-routes/workspaces)
    (context* "/users" []
      :tags ["users"]
      user-routes/users)
    (context* "/tool-requests" []
      :tags ["tool-requests"]
      tool-routes/tool-requests)
    (context* "/reference-genomes" []
      :tags ["reference-genomes"]
      reference-genome-routes/reference-genomes)
    (context* "/oauth" []
      :tags ["oauth-routes"]
      oauth-routes/oauth)
    (context* "/collaborators" []
      :tags ["collaborator-routes"]
      collaborator-routes/collaborators)
    (context* "/admin/apps" []
      :tags ["admin-apps"]
      admin-routes/admin-apps)
    (context* "/admin/apps/categories" []
      :tags ["admin-categories"]
      admin-routes/admin-categories)
    (context* "/admin/reference-genomes" []
      :tags ["admin-reference-genomes"]
      admin-routes/reference-genomes)
    (context* "/admin/tools/container-images" []
      :tags ["admin-container-images"]
      tool-routes/container-images)
    (context* "/admin/tools/data-containers" []
      :tags ["admin-data-containers"]
      tool-routes/admin-data-containers)
    (context* "/admin/tools" []
      :tags ["admin-tools"]
      tool-routes/admin-tools)
    (context* "/admin/tool-requests" []
      :tags ["admin-tool-requests"]
      admin-routes/admin-tool-requests)
    (route/not-found (service/unrecognized-path-response))))
