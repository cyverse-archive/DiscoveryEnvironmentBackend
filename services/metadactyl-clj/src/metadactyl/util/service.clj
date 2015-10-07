(ns metadactyl.util.service
  (:use [clojure.java.io :only [reader]]
        [ring.util.response :only [charset]]
        [ring.util.http-response :only [ok]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.assertions :as ca]
            [metadactyl.util.coercions :as mc]))

(defn unrecognized-path-response
  "Builds the response to send for an unrecognized service path."
  []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:reason msg})))

(defn parse-json
  "Parses a JSON request body."
  [body]
  (try+
    (if (string? body)
      (cheshire/decode body true)
      (cheshire/decode-stream (reader body) true))
    (catch Exception e
      (throw+ {:type :clojure-commons.exception/invalid-json
               :error     (str e)}))))

(defn coerced-trap
  "Traps a service call, automatically coercing the output and calling success-response
   on the result."
  [_ schema func & args]
  (ok (mc/coerce! schema (apply func args))))

(def not-found ca/not-found)
(def not-owner ca/not-owner)
(def not-unique ca/not-unique)
(def bad-request ca/bad-request)
(def assert-found ca/assert-found)
(def assert-valid ca/assert-valid)
(def request-failure ca/request-failure)
