(ns data-info.routes
  (:use [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.middleware :only [log-validation-errors]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.api.legacy]
        [ring.util.response :only [redirect]])
  (:require [compojure.route :as route]
            [data-info.routes.data :as data-routes]
            [data-info.routes.exists :as exists-routes]
            [data-info.routes.home :as home-routes]
            [data-info.routes.navigation :as navigation-routes]
            [data-info.routes.status :as status-routes]
            [data-info.routes.stats :as stat-routes]
            [data-info.util :as util]
            [data-info.util.config :as config]
            [data-info.util.service :as svc]
            [ring.middleware.keyword-params :as params]
            [service-logging.thread-context :as tc]))

(defn context-middleware
  [handler]
  (tc/wrap-thread-context handler config/svc-info))

(defapi app
  (swagger-ui config/docs-uri)
  (swagger-docs
    {:info {:title "Discovery Environment Data Info API"
            :description "Documentation for the Discovery Environment Data Info REST API"
            :version "2.0.0"}
     :tags [{:name "service-info", :description "Service Information"}
            {:name "data-by-id", :description "Data Operations (by ID)"}
            {:name "data", :description "Data Operations"}
            {:name "bulk", :description "Bulk Operations"}
            {:name "navigation", :description "Navigation"}
            {:name "home", :description "User Home Directories"}]})
  (middlewares
    [tc/add-user-to-context
     wrap-query-params
     wrap-lcase-params
     params/wrap-keyword-params
     util/req-logger
     context-middleware
     log-validation-errors]
    status-routes/status
    data-routes/data-operations
    exists-routes/existence-marker
    home-routes/home
    navigation-routes/navigation
    stat-routes/stat-gatherer
    (route/not-found (svc/unrecognized-path-response))))
