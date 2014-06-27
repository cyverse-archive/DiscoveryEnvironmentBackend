(ns donkey.routes.user-info
  (:use [compojure.core]
        [donkey.services.user-info]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (GET "/user-search" [:as {:keys [headers params]}]
        (trap #(user-search params headers)))

   (GET "/user-info" [:as {params :params}]
        (trap #(user-info (as-vector (:username params)))))))
