(ns clojure-commons.exception
  (:require [clojure-commons.error-codes :as ec]
            [cheshire.core :as cheshire]
            [ring.util.response :as header]
            [ring.util.http-response :as resp]))

(defn- embedErrorInfo
  [throwable exception response]
  (assoc response
    :throwable throwable
    :exception exception))

(defn as-de-exception-handler
  "Wraps a compojure-api exception handler function and performs
   the following tasks;
     - creates an exception map from the wrapped function's response
     - json encodes the exception map into the response body
     - adds json headers to response map

   This function expects the error-handler to return a ring response with
   a ':errors' subkey in the response ':body'"
  [error-handler error-code]
  (fn [error error-type request]
    (let [response (error-handler error error-type request)
          exception {:error_code error-code
                     :reason (get-in response [:body :errors])}]
      (header/content-type
        (embedErrorInfo error
                        exception
                        (assoc response
                          :body (cheshire/encode exception)))
        "application/json; charset=utf-8"))))

(defn authentication-not-found-handler
  [error data _]
  (resp/unauthorized
    (let [exception {:error_code ec/ERR_NOT_AUTHORIZED
                     :reason (:error data)}]
      (header/header
        (embedErrorInfo error
                        exception
                        (resp/unauthorized (cheshire/encode exception)))
        "WWW-Authenticate"
        "Custom"))))

(defn invalid-cfg-handler
  [error error-type _]
  (let [exception {:error_code ec/ERR_CONFIG_INVALID
                   :reason (:error error-type)}]
    (embedErrorInfo error
                    exception
                    (resp/internal-server-error (cheshire/encode exception)))))

(defn unchecked-handler
  [error error-type _]
  (cond
    (ec/error? error-type) (embedErrorInfo error
                                           error-type
                                           (resp/internal-server-error (cheshire/encode error-type)))

    (instance? Object error) (let [exception {:error_code ec/ERR_UNCHECKED_EXCEPTION
                                              :reason (:message error-type)}]
                               (embedErrorInfo error
                                               exception
                                               (resp/internal-server-error (cheshire/encode exception))))))


