(ns donkey.routes.comments
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.services.metadata.comments :as comments]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc])
  (:import [java.util UUID]))


(defn- handle-add-comment
  [entry-id body]
  (let [comment (-> body slurp (json/parse-string true) :comment)]
    (comments/add-comment (config/jargon-cfg)
                          (:shortUsername user/current-user)
                          (UUID/fromString entry-id)
                          comment)))


(defn- handle-get-comments
  [entry-id]
  (comments/list-comments (config/jargon-cfg)
                          (:shortUsername user/current-user)
                          (UUID/fromString entry-id)))


(defn- handle-retract-comment
  [entry-id comment-id retracted]
  (comments/update-retract-status (config/jargon-cfg)
                                  (:shortUsername user/current-user)
                                  (UUID/fromString entry-id)
                                  (UUID/fromString comment-id)
                                  (Boolean/parseBoolean retracted)))


(defn secured-comment-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/comments" [entry-id]
      (handle-get-comments entry-id))

    (POST "/filesystem/entry/:entry-id/comments" [entry-id :as {body :body}]
      (util/trap #(handle-add-comment entry-id body)))

    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id" [entry-id comment-id retracted]
      (util/trap #(handle-retract-comment entry-id comment-id retracted)))))
