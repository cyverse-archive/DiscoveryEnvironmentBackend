(ns clojure-commons.middleware
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn log-request
  [{:keys [request-method uri] :as request}]
  (let [method (string/upper-case (name request-method))]
    (log/log 'AccessLogger :info nil (str method " " uri))))

(defn wrap-log-requests
  [handler]
  (fn [request]
    (log-request request)
    (handler request)))

(defn log-validation-errors
  [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :ring.swagger.schema/validation] {:keys [error]}
       (log/error (:throwable &throw-context) error)
       (throw+)))))
