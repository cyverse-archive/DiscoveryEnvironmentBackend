(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.services :as svc]))


(defn secured-favorite-routes
  []
  (util/optional
    [config/metdata-routes-enabled]

    (PUT "/favorites/filesystem/:entry-id" entry-id
      ;; TODO implement
      (svc/success-response))

    (DELETE "/favorites/filesystem/:entry-id" entry-id
      ;; TODO implement
      (svc/success-response))

    (GET "/favorites/filesystem"
      ;; TODO implement
      (svc/success-response {:filesystem ["f81d4fae-7dec-11d0-a765-00a0c91e6bf7"
                                          "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"]}))

    (POST "/favorites/filter"
      ;; TODO implement
      (svc/success-response {:analyses   []
                             :apps       []
                             :filesystem ["f81d4fae-7dec-11d0-a765-00a0c91e6bf7"
                                          "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"]}))))