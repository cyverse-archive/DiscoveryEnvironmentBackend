(ns metadactyl.util.service
  (:use [clojure.java.io :only [reader]]
        [ring.util.response :only [charset]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
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

(defn build-url
  "Builds a URL from a base URL and one or more URL components."
  [base & components]
  (string/join "/" (map #(.replaceAll % "^/|/$" "")
                        (cons base components))))

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

(defn not-found
  "Throws an exception indicating that an object wasn't found."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_FOUND
           :reason     (string/join " " [desc id "not found"])}))

(defn not-owner
  "Throws an exception indicating that the user isn't permitted to perform the requested option."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_OWNER
           :reason     (str "authenticated user doesn't own " desc ", " id)}))

(defn not-unique
  "Throws an exception indicating that multiple objects were found when only one was expected."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_UNIQUE
           :reason     (string/join " " [desc id "not unique"])}))

(defn bad-request
  "Throws an exception indicating that the incoming request is invalid."
  [reason]
  (throw+ {:error_code ce/ERR_BAD_REQUEST
           :reason     reason}))

(defn assert-found
  "Asserts that an object to modify or retrieve was found."
  [obj desc id]
  (if (nil? obj)
    (not-found desc id)
    obj))

(defn assert-valid
  "Throws an exception if an arbitrary expression is false."
  [valid? & msgs]
  (when-not valid?
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message    (string/join " " msgs)})))

(defn request-failure
  "Throws an exception indicating that a request failed for an unexpected reason."
  [& msgs]
  (throw+ {:error_code ce/ERR_REQUEST_FAILED
           :message    (string/join " " msgs)}))
