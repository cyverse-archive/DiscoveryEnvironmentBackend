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


(defn secured-comment-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/comments" [:as req]
      ;; TODO implement
      (svc/success-response {:comments [{:id           "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"
                                         :commenter    "tedgin"
                                         :comment_time 1000000000000
                                         :retracted    true
                                         :comment      "Your momma smells like a gopher!"}
                                        {:id           "f81d4fae-7dec-11d0-a765-00a0c91e6bf7"
                                         :commenter    "tedgin"
                                         :comment_time 1000000000001
                                         :retracted    false
                                         :comment      "I shouldn't have said that."}]}))

    (POST "/filesystem/entry/:entry-id/comments" [entry-id :as {body :body}]
      (util/trap #(handle-add-comment entry-id body)))

    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id" [:as req]
      ;; TODO implement
      (svc/success-response))))
