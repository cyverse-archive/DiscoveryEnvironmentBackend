(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.services.metadata.favorites :as fave]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc])
  (:import [java.util UUID]))


(defn- handle-filter
  [fs-cfg user body]
  (let [ids-txt (-> body slurp (json/parse-string true) :filesystem)
        uuids   (->> ids-txt (map #(UUID/fromString %)) set)]
    (fave/filter-favorites fs-cfg user uuids)))


(defn secured-favorites-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (PUT "/favorites/filesystem/:entry-id" [entry-id]
      (util/trap #(fave/add-favorite (config/jargon-cfg)
                                     (:shortUsername user/current-user)
                                     (UUID/fromString entry-id))))

    (DELETE "/favorites/filesystem/:entry-id" [entry-id]
      (util/trap #(fave/remove-favorite (:shortUsername user/current-user)
                                        (UUID/fromString entry-id))))

    (GET "/favorites/filesystem" []
      (util/trap #(fave/list-favorite-data (config/jargon-cfg) (:shortUsername user/current-user))))

    (POST "/favorites/filter" [:as {body :body}]
      (util/trap #(handle-filter (config/jargon-cfg) (:shortUsername user/current-user) body)))))
