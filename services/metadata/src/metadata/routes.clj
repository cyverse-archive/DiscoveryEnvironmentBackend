(ns metadata.routes
  (:use [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.core :only [wrap-routes]]
        [service-logging.middleware :only [wrap-logging add-user-to-context clean-context]]
        [common-swagger-api.schema])
  (:require [metadata.routes.avus :as avu-routes]
            [metadata.routes.comments :as comment-routes]
            [metadata.routes.favorites :as favorites-routes]
            [metadata.routes.status :as status-routes]
            [metadata.routes.tags :as tag-routes]
            [metadata.routes.templates :as template-routes]
            [metadata.util.config :as config]
            [ring.middleware.keyword-params :as params]
            [clojure-commons.exception :as cx]))

(defapi app
  {:exceptions cx/exception-handlers}
  (swagger-ui config/docs-uri)
  (swagger-docs
    {:info {:title "Discovery Environment Metadata API"
            :description "Documentation for the Discovery Environment Metadata REST API"
            :version "2.0.0"}
     :tags [{:name "service-info", :description "Service Information"}
            {:name "avus", :description "Attribute/Value/Unit Management"}
            {:name "data-comments", :description "Comments on Data Items"}
            {:name "app-comments", :description "Comments on Apps"}
            {:name "favorites", :description "Favorite Resources"}
            {:name "admin-data-comments", :description "Admin Data Item Comment Management"}
            {:name "admin-app-comments", :description "Admin App Comment Management"}
            {:name "tags", :description "Tags Management"}
            {:name "template-info", :description "Template Information"}
            {:name "template-administration", :description "Template Administration"}]})
  (middlewares
    [clean-context
     wrap-query-params
     wrap-lcase-params
     params/wrap-keyword-params
     add-user-to-context
     wrap-logging]
    status-routes/status
    avu-routes/avus
    comment-routes/data-comment-routes
    comment-routes/app-comment-routes
    comment-routes/admin-data-comment-routes
    comment-routes/admin-app-comment-routes
    favorites-routes/favorites
    tag-routes/filesystem-tags
    tag-routes/tags
    template-routes/templates
    template-routes/admin-templates))
