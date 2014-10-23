(ns donkey.routes.user-info
  (:use [compojure.core]
        [donkey.services.user-info]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-user-info-routes
  []
  (optional-routes
   [config/user-info-routes-enabled]

   (GET "/user-search" [:as {:keys [uri headers params]}]
        (ce/trap uri #(user-search params headers)))

   (GET "/user-info" [:as {:keys [uri params]}]
        (ce/trap uri #(user-info (as-vector (:username params)))))))
