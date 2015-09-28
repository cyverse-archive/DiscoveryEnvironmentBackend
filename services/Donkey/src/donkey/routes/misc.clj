(ns donkey.routes.misc
  (:use [compojure.core]
        [donkey.util])
  (:require [clojure.string :as string])
  (:import [java.util UUID]))

(defn unsecured-misc-routes
  []
  (routes
    (GET "/" [request]
      "Welcome to Donkey!  I've mastered the stairs!\n")

    (GET "/uuid" []
      (string/upper-case (str (UUID/randomUUID))))))
