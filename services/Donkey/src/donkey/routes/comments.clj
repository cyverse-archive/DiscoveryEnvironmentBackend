(ns donkey.routes.comments
  (:use [compojure.core :only [DELETE GET PATCH POST]])
  (:require [donkey.services.metadata.comments :as comments]
            [donkey.util :as util]
            [donkey.util.config :as config]))

(defn data-comment-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/filesystem/entry/:entry-id/comments" [entry-id]
         (comments/list-data-comments entry-id))

    (POST "/filesystem/entry/:entry-id/comments" [entry-id :as {body :body}]
          (comments/add-data-comment entry-id body))

    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id"
           [entry-id comment-id retracted]
           (comments/update-data-retract-status entry-id comment-id retracted))))

(defn admin-data-comment-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (DELETE "/filesystem/entry/:entry-id/comments/:comment-id"
      [entry-id comment-id]
      (comments/delete-data-comment entry-id comment-id))))

(defn app-comment-routes
  []
  (util/optional-routes
    [#(and (config/app-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/apps/:app-id/comments" [app-id]
      (comments/list-app-comments app-id))

    (POST "/apps/:app-id/comments" [app-id :as {body :body}]
      (comments/add-app-comment app-id body))

    (PATCH "/apps/:app-id/comments/:comment-id"
      [app-id comment-id retracted]
      (comments/update-app-retract-status app-id comment-id retracted))))

(defn admin-app-comment-routes
  []
  (util/optional-routes
    [#(and (config/app-routes-enabled) (config/metadata-routes-enabled))]

    (DELETE "/apps/:app-id/comments/:comment-id"
      [app-id comment-id]
      (comments/delete-app-comment app-id comment-id))))
