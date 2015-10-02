(ns clojure-commons.exception-util
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.exception :as cx]))

(defn unauthorized
  "Throws an error indicating that the request is unauthorized."
  [reason]
  (throw+ {:type ::cx/authentication-not-found :error reason}))

(defn forbidden
  "Throws an error indicating that the request is forbidden."
  [reason]
  (throw+ {:type ::cx/forbidden :error reason}))
