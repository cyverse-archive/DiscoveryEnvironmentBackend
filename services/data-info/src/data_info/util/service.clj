(ns data-info.util.service
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [data-info.util.config :as config]))

(def ^:private default-content-type
  "application/json; charset=utf-8")

(defn unrecognized-path-response
  "Builds the response to send for an unrecognized service path."
  []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:success false :reason msg})))

(defmacro log-runtime
  [[msg] & body]
  `(let [start#  (System/currentTimeMillis)
         result# (do ~@body)
         finish# (System/currentTimeMillis)]
     (when (config/log-runtimes)
       (log/warn ~msg "-" (- finish# start#) "milliseconds"))
     result#))

(defn- success-response*
  [body]
  {:status  200
   :body    body
   :headers {"Content-Type" default-content-type}})

(defn trap
  "Traps a service call, automatically calling success-response on the result."
  [action func & args]
  (ce/trap action #(success-response* (apply func args))))
