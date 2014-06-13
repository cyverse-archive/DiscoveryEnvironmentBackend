(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [donkey.auth.user-attributes :as user]
            [donkey.services.metadata.favorites :as fave]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc])
  (:import [java.util UUID]))


(defn secured-favorites-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (PUT "/favorites/filesystem/:entry-id" [entry-id]
      (util/trap #(fave/add-favorite (config/jargon-cfg)
                                     (:shortUsername user/current-user)
                                     (UUID/fromString entry-id))))

    (DELETE "/favorites/filesystem/:entry-id" [entry-id]
      ;; TODO implement
      (svc/success-response))

    (GET "/favorites/filesystem" []
      ;; TODO implement
      (svc/success-response {:filesystem ["f81d4fae-7dec-11d0-a765-00a0c91e6bf7"
                                          "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"]}))

    (POST "/favorites/filter" []
      ;; TODO implement
      (svc/success-response {:analyses   []
                             :apps       []
                             :filesystem ["f81d4fae-7dec-11d0-a765-00a0c91e6bf7"
                                          "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"]}))))