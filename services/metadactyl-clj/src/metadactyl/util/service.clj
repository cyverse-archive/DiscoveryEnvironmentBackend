(ns metadactyl.util.service
  (:use [clojure.java.io :only [reader]]
        [ring.util.response :only [charset]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.assertions :as ca]
            [clojure-commons.error-codes :as ce]
            [metadactyl.util.coercions :as mc]))

(def ^:private default-content-type-header
  {"Content-Type" "application/json; charset=utf-8"})

(defn success-response
  ([map]
     (charset
      {:status       200
       :body         map
       :headers default-content-type-header}
      "UTF-8"))
  ([]
     (success-response nil)))

(defn not-found-response
  [msg]
  (charset
   {:status 404
    :body   msg}
   "UTF-8"))

(defn unrecognized-path-response
  "Builds the response to send for an unrecognized service path."
  []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:reason msg})))

(defn prepare-forwarded-request
  "Prepares a request to be forwarded to a remote service."
  [request body]
  {:headers (dissoc (:headers request) "content-length")
   :body body})

(defn parse-json
  "Parses a JSON request body."
  [body]
  (try+
    (if (string? body)
      (cheshire/decode body true)
      (cheshire/decode-stream (reader body) true))
    (catch Exception e
      (throw+ {:error_code ce/ERR_INVALID_JSON
               :detail     (str e)}))))

(defn trap
  "Traps a service call, automatically calling success-response on the result."
  [action func & args]
  (ce/trap action #(success-response (apply func args))))

(defn coerced-trap
  "Traps a service call, automatically coercing the output and calling success-response
   on the result."
  [action schema func & args]
  (ce/trap action #(success-response (mc/coerce! schema (apply func args)))))

(def not-found ca/not-found)
(def not-owner ca/not-owner)
(def not-unique ca/not-unique)
(def bad-request ca/bad-request)
(def assert-found ca/assert-found)
(def assert-not-found ca/assert-not-found)
(def assert-valid ca/assert-valid)
(def request-failure ca/request-failure)
