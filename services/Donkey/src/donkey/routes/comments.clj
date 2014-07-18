(ns donkey.routes.comments
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [donkey.services.metadata.comments :as comments]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-comment-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/filesystem/entry/:entry-id/comments" [entry-id]
      (util/trap #(comments/list-comments entry-id)))

    (POST "/filesystem/entry/:entry-id/comments" [entry-id :as {body :body}]
      (util/trap #(comments/add-comment entry-id body)))

    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id" [entry-id comment-id retracted]
      (util/trap #(comments/update-retract-status entry-id comment-id retracted)))))
