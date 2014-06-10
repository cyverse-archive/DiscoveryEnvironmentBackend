(ns donkey.routes.tags
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [donkey.auth.user-attributes :as user]
            [donkey.services.metadata.tags :as tags]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]))


(defn secured-tag-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/tags" [:as req]
      (svc/success-response {:tags ["user/username/tag+1" "user/username/tag+2"]}))

    (PATCH "/filesystem/entry/:entry-id/tags" [entry-id :as {params :params body :body}]
      (svc/success-response))

    (GET "/tags/suggestions" [contains]
      (util/trap #(tags/suggest-tags (:shortUsername user/current-user) contains)))

    (POST "/tags/user" [:as {body :body}]
      (util/trap #(tags/create-user-tag (:shortUsername user/current-user) body)))

    (PATCH "/tags/user/:tag-id" [tag-id :as {body :body}]
      (util/trap #(tags/update-user-tag (:shortUsername user/current-user) tag-id body)))))
