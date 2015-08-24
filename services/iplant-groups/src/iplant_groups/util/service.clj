(ns iplant_groups.util.service
  (:use [ring.util.response :only [charset]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.error-codes :as ce]))

(def ^:private default-content-type-header
  {"Content-Type" "application/json; charset=utf-8"})

(defn success-response
  [map]
  (charset
   {:status  200
    :body    map
    :headers default-content-type-header}
   "UTF-8"))

(defn not-found
  [desc id]
  (throw+ {:error_code  ce/ERR_NOT_FOUND
           :description desc
           :id          id}))

(defn trap
  "Traps a service call, automatically calling success-response on the result."
  [action func & args]
  (ce/trap action #(success-response (apply func args))))

(defn parse-json
  "Parses JSON encoded text in either a string or an input stream."
  [json]
  (if (string? json)
    (cheshire/parse-string json true)
    (cheshire/parse-stream json true)))
