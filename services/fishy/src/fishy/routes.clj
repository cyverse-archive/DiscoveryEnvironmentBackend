(ns fishy.routes
  (:use [clojure-commons.middleware :only [log-validation-errors]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [fishy.routes.domain.group]
        [ring.middleware.keyword-params :only [wrap-keyword-params]])
  (:require [fishy.routes.folders :as folder-routes]
            [fishy.routes.groups :as group-routes]
            [fishy.routes.status :as status-routes]
            [fishy.routes.subjects :as subject-routes]
            [fishy.util.config :as config]
            [service-logging.thread-context :as tc]))

(defapi app
  (swagger-ui config/docs-uri)
  (swagger-docs
   {:info {:title       "RESTful Service Facade for Grouper"
           :description "Documentation for the Fishy API"
           :version     "2.0.0"}
    :tags [{:name "folders", :description "Folder Information"}
           {:name "groups", :description "Group Information"}
           {:name "service-info", :description "Service Status Information"}
           {:name "subjects", :description "Subject Information"}]})
  (middlewares
   [wrap-keyword-params
    wrap-query-params
    (tc/wrap-thread-context config/svc-info)
    log-validation-errors]
   (context* "/" []
    :tags ["service-info"]
    status-routes/status)
   (context* "/folders" []
    :tags ["folders"]
    folder-routes/folders)
   (context* "/groups" []
    :tags ["groups"]
    group-routes/groups)
   (context* "/subjects" []
    :tags ["subjects"]
    subject-routes/subjects)))
