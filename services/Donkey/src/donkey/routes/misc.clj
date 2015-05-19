(ns donkey.routes.misc
  (:use [compojure.core]
        [donkey.util]
        [clojure-commons.middleware :only [wrap-log-requests]])
  (:require [clojure.string :as string])
  (:import [java.util UUID]))

(defn unsecured-misc-routes
  []
  (wrap-log-requests
    (routes
      (GET "/" [request]
        "Welcome to Donkey!  I've mastered the stairs!\n")

      (GET "/uuid" []
           (string/upper-case (str (UUID/randomUUID)))))))
