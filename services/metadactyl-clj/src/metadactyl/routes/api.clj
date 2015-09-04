(ns metadactyl.routes.api
  (:use [clojure-commons.middleware :only [log-validation-errors]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.analysis.listing]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.app.element]
        [metadactyl.routes.domain.app.rating]
        [metadactyl.routes.domain.callback]
        [metadactyl.routes.domain.collaborator]
        [metadactyl.routes.domain.oauth]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.domain.user]
        [metadactyl.routes.domain.workspace]
        [metadactyl.routes.params]
        [metadactyl.schema.containers]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [ring.swagger.json-schema :only [json-type]]
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
            [service-logging.thread-context :as tc]))

(defmethod json-type schema.core.AnythingSchema [_] {:type "any"})

(def context-map (ref {}))

(defn set-context-map!
  "Sets the map that will be used to create the ThreadContext by wrap-context-map."
  [cm]
  (dosync (ref-set context-map cm)))

(defn wrap-context-map
  "Sets the ThreadContext for each request."
  [handler]
  (fn [request]
    (tc/set-context! @context-map)
    (let [resp (handler request)]
      (tc/clear-context!)
      resp)))

(defapi app
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
            {:name "admin-data-containers", :description "Admin Docker Data Container endpoints."}
            {:name "admin-tools", :description "Admin Tool endpoints."}
            {:name "admin-reference-genomes", :description "Admin Reference Genome endpoints."}
            {:name "admin-tool-requests", :description "Admin Tool Request endpoints."}]})
  (middlewares
    [wrap-keyword-params
     wrap-query-params
     wrap-context-map
     log-validation-errors]
    (context* "/" []
      :tags ["service-info"]
      status-routes/status)
    (context* "/callbacks" []
      :tags ["callbacks"]
      callback-routes/callbacks))
  (middlewares
    [wrap-keyword-params
     wrap-query-params
     tc/add-user-to-context
     wrap-context-map
     log-validation-errors
     store-current-user]
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
