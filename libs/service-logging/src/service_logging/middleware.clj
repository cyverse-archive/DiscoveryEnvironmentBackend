(ns service-logging.middleware
  (:use [slingshot.slingshot :only [try+ throw+]] )
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [service-logging.thread-context :as tc]))

(defn flatten-map
  "Takes a nested map, and flattens it. The nested keys are combined with
  the given separator, and the keys can optionally be pre-prended with a string"
  [form separator pre]
  (into {}
        (mapcat (fn [[k v]]
                  (let [prefix (if pre (str pre separator (name k)) (name k))]
                    (if (map? v)
                      (flatten-map v separator prefix)
                      [[(keyword prefix) v]])))
                form)))

; TODO: Setup logger level conditional logging, which logs the body if the log
;       level is at it's highest
(defn- clean-request
  [request]
  (dissoc request
          :body
          :user-info))

(defn- clean-response
  [response]
  (dissoc response
          :body))

(defn log-request
  [{:keys [request-method uri] :as request}]
  (let [method (string/upper-case (name request-method))]
    (tc/with-logging-context {:request (json/write-str (clean-request request))}
                             (log/log 'AccessLogger :info nil (str method " " uri)))))

(defn log-response
  ([level throwable {:keys [request-method uri]} response]
   (let [method (string/upper-case (name request-method))]
     (tc/with-logging-context {:response (json/write-str (clean-response (assoc response
                                                                           :uri uri
                                                                           :request-method request-method)))}
                              (log/log 'AccessLogger level throwable (str method " " uri)))))
  ([request response]
    (log-response :info nil request response))
  ([level request response]
   (log-response level nil request response)))

(defn wrap-logging
  "Logs incoming requests and their responses with the 'AccessLogger' logger.
   Neither the request nor the response bodies are logged."
  [handler]
  (fn [request]
    (log-request request)
    (let [response (handler request)]
      (log-response request response)
      response)))

(defn wrap-log-requests
  "Logs incoming requests with the 'AccessLogger' logger.
   The request body is not logged."
  [handler]
  (fn [request]
    (log-request request)
    (handler request)))

(defn wrap-log-responses
  "Logs responses with the 'AccessLogger' logger.
   The reponse body is not logged."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (log-response request response)
      response)))

(defn log-validation-errors
  [handler]
  (fn [request]
    (try+
      (handler request)
      (catch [:type :ring.swagger.schema/validation] {:keys [error]}
        (log/error (:throwable &throw-context) error)
        (throw+)))))
