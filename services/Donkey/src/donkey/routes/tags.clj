(ns donkey.routes.tags
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.services.metadata.tags :as tags]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc])
  (:import [java.util UUID]))


(defn- handle-patch-file-tags
  [fs-cfg user entry-id type body]
  (let [req  (-> body slurp (json/parse-string true))
        mods (map #(UUID/fromString %) (:tags req))]
    (condp = type
      "attach" (tags/attach-tags fs-cfg user entry-id mods)
      "detach" (tags/detach-tags fs-cfg user entry-id mods)
               (svc/donkey-response {} 400))))



(defn secured-tag-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/tags" [entry-id]
      (util/trap #(tags/list-attached-tags (config/jargon-cfg)
                                           (:shortUsername user/current-user)
                                           (UUID/fromString entry-id))))

    (PATCH "/filesystem/entry/:entry-id/tags" [entry-id type :as {body :body}]
      (util/trap #(handle-patch-file-tags (config/jargon-cfg)
                                          (:shortUsername user/current-user)
                                          (UUID/fromString entry-id)
                                          type
                                          body)))

    (GET "/tags/suggestions" [contains]
      (util/trap #(tags/suggest-tags (:shortUsername user/current-user) contains)))

    (POST "/tags/user" [:as {body :body}]
      (util/trap #(tags/create-user-tag (:shortUsername user/current-user) body)))

    (PATCH "/tags/user/:tag-id" [tag-id :as {body :body}]
      (util/trap #(tags/update-user-tag (:shortUsername user/current-user) tag-id body)))))
