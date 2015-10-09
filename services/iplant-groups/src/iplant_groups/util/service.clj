(ns iplant_groups.util.service
  (:use [ring.util.response :only [charset]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.error-codes :as ce]))

(defn not-found
  [desc id]
  (throw+ {:error_code  ce/ERR_NOT_FOUND
           :description desc
           :id          id}))

(defn forbidden
  [desc id]
  (throw+ {:error_code  ce/ERR_FORBIDDEN
           :description desc
           :id          id}))

(defn parse-json
  "Parses JSON encoded text in either a string or an input stream."
  [json]
  (if (string? json)
    (cheshire/parse-string json true)
    (cheshire/parse-stream json true)))
