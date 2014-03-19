(ns conrad.common
  (:use [clojure.string :only (blank? upper-case)])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log])
  (:import [java.sql SQLException]
           [java.util UUID]))

(def json-content-type "application/json")

(defn success-response [map]
  {:status 200
   :body (cheshire/encode (merge {:success true} map))
   :content-type json-content-type})

(defn failure-response [e]
  (log/error e "internal error")
  {:status 400
   :body (cheshire/encode {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn log-next-exception [e]
  (if (and (instance? SQLException e) (.getNextException e))
    (log/error (.getNextException e) "Next exception.")
    (recur (.getNextException e))))

(defn error-response [e]
  (log/error e "bad request")
  {:status 500
   :body (cheshire/encode {:success false :reason (.getMessage e)})
   :content-type json-content-type})

(defn unrecognized-path-response []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:success false :reason msg})))

(defn unauthorized-response [url]
  (log/warn (str "unauthorized request: " url))
  {:status 401
   :body (cheshire/encode {:success false :reason "UNAUTHORIZED"})})

(defn extract-required-field [obj field-name]
  (let [value (get obj field-name)]
    (if (nil? value)
      (throw (IllegalArgumentException.
              (str "missing required field, " field-name ", in request"))))
    value))

(defn uuid []
  (upper-case (str (java.util.UUID/randomUUID))))

(defn force-long [value field]
  (when-not (blank? value)
    (try
      (Long/parseLong (str value))
      (catch NumberFormatException e
        (throw (IllegalArgumentException.
                (str "invalid integer value, " value ", in field, " (name field))))))))
