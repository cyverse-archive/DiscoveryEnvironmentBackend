(ns donkey.routes.comments
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]))


(defn secured-comment-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/comments" [:as req]
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

    (POST "/filesystem/entry/:entry-id/comments" [:as req]
      (svc/success-response {:id "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"}))

    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id" [:as req]
      (svc/success-response))))
