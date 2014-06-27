(ns donkey.routes.tags
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [donkey.services.metadata.tags :as tags]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-tag-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/filesystem/entry/:entry-id/tags" [entry-id]
      (util/trap #(tags/list-attached-tags entry-id)))

    (PATCH "/filesystem/entry/:entry-id/tags" [entry-id type :as {body :body}]
      (util/trap #(tags/handle-patch-file-tags entry-id type body)))

    (GET "/tags/suggestions" [contains limit]
      (util/trap #(tags/suggest-tags contains limit)))

    (POST "/tags/user" [:as {body :body}]
      (util/trap #(tags/create-user-tag body)))

    (PATCH "/tags/user/:tag-id" [tag-id :as {body :body}]
      (util/trap #(tags/update-user-tag tag-id body)))))
