(ns donkey.routes.user-info
  (:use [compojure.core]
        [donkey.services.user-info]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (GET "/user-search" [:as {:keys [params]}]
        (user-search (:search params)))

   (GET "/user-info" [:as {:keys [params]}]
        (user-info (as-vector (:username params))))))
