(ns fishy.routes
  (:use [clojure-commons.middleware :only [log-validation-errors]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [fishy.routes.domain.group]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [fishy.routes.status :as status-routes]
            [fishy.util.config :as config]
            [service-logging.thread-context :as tc]))

(defapi app
  (swagger-ui config/docs-uri)
  (swagger-docs
   {:info {:title       "RESTful Service Facade for Grouper"
           :description "Documentation for the Fishy API"
           :version     "2.0.0"}
    :tags [{:name "service-info", :description "Service Status Information"}]})
  (middlewares
   [wrap-keyword-params
    wrap-query-params
    (tc/wrap-thread-context config/svc-info)
    log-validation-errors]
   (context* "/" []
    :tags ["service-info"]
    status-routes/status)))
