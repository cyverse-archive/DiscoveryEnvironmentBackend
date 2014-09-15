(ns metadactyl.routes.api
  (:use [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.api.legacy]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.app.element]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.domain.tool-requests]
        [metadactyl.routes.params]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [ring.swagger.schema :only [describe]]
        [ring.util.response :only [redirect]])
  (:require [metadactyl.routes.admin :as admin-routes]
            [metadactyl.routes.apps :as app-routes]
            [metadactyl.routes.apps.categories :as app-category-routes]
            [metadactyl.routes.apps.elements :as app-element-routes]
            [metadactyl.routes.tool-requests :as tool-request-routes]
            [metadactyl.routes.legacy :as legacy-routes]))

(defapi app
  (swagger-ui "/api")
  (swagger-docs "/api/api-docs"
    :title "Discovery Environment Apps API"
    :description "Documentation for the Discovery Environment Apps REST API"
    :apiVersion "2.0.0")
  (GET "/" [] (redirect "/api"))
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
    (swaggered "element-types"
      :description "App Element endpoints."
      (context "/apps/elements" [] app-element-routes/app-elements))
    (swaggered "tool-requests"
      :description "Tool Request endpoints."
      (context "/tool-requests" [] tool-request-routes/tool-requests))
    (swaggered "admin-apps"
      :description "Admin App endpoints."
      (context "/admin/apps" [] admin-routes/admin-apps))
    (swaggered "admin-tool-requests"
      :description "Admin Tool Request endpoints."
      (context "/admin/tool-requests" [] admin-routes/tool-requests))
    (swaggered "secured"
      :description "Secured App endpoints."
      (context "/secured" [] legacy-routes/secured-routes))
    (swaggered "unsecured"
      :description "Unsecured App endpoints."
      legacy-routes/metadactyl-routes)))
