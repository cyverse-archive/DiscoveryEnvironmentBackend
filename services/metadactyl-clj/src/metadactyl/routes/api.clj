(ns metadactyl.routes.api
  (:use [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.api.legacy]
        [metadactyl.routes.domain.analysis]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.app.element]
        [metadactyl.routes.domain.app.rating]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [ring.swagger.schema :only [describe]]
        [ring.util.response :only [redirect]])
  (:require [metadactyl.routes.admin :as admin-routes]
            [metadactyl.routes.analyses :as analysis-routes]
            [metadactyl.routes.apps :as app-routes]
            [metadactyl.routes.apps.categories :as app-category-routes]
            [metadactyl.routes.apps.elements :as app-element-routes]
            [metadactyl.routes.apps.pipelines :as pipeline-routes]
            [metadactyl.routes.reference-genomes :as reference-genome-routes]
            [metadactyl.routes.tools :as tool-routes]
            [metadactyl.routes.legacy :as legacy-routes]))

(defapi app
  (swagger-ui "/api")
  (swagger-docs "/api/api-docs"
    :title "Discovery Environment Apps API"
    :description "Documentation for the Discovery Environment Apps REST API"
    :apiVersion "2.0.0")
  (GET "/" [] (redirect "/api"))
  (GET "/favicon.ico" [] {:status 404})
  (middlewares
    [wrap-keyword-params
     wrap-query-params
     store-current-user]
    (swaggered "app-categories"
      :description "App Category endpoints."
      (context "/apps/categories" [] app-category-routes/app-categories))
    (swaggered "apps"
      :description "App endpoints."
      (context "/apps" [] app-routes/apps))
    (swaggered "pipelines"
      :description "Pipeline endpoints."
      (context "/apps/pipelines" [] pipeline-routes/pipelines))
    (swaggered "element-types"
      :description "App Element endpoints."
      (context "/apps/elements" [] app-element-routes/app-elements))
    (swaggered "analyses"
      :description "Analysis endpoints."
      (context "/analyses" [] analysis-routes/analyses))
    (swaggered "tools"
      :description "Tool endpoints."
      tool-routes/tools)
    (swaggered "reference-genomes"
      :description "Reference Genome endpoints."
      (context "/reference-genomes" [] reference-genome-routes/reference-genomes))
    (swaggered "admin-apps"
      :description "Admin App endpoints."
      (context "/admin/apps" [] admin-routes/admin-apps))
    (swaggered "admin-tools"
      :description "Admin Tool endpoints."
      (context "/admin" [] admin-routes/tools))
    (swaggered "admin-reference-genomes"
      :description "Admin Reference Genome endpoints."
      (context "/admin/reference-genomes" [] admin-routes/reference-genomes))
    (swaggered "secured"
      :description "Secured App endpoints."
      (context "/secured" [] legacy-routes/secured-routes))
    (swaggered "unsecured"
      :description "Unsecured App endpoints."
      legacy-routes/metadactyl-routes)))
