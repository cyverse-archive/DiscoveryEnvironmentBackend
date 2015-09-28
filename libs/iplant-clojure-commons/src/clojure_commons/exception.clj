(ns clojure-commons.exception
  (:use [ring.util.response :only [content-type]])
  (:require [clojure-commons.error-codes :as ec]
            [cheshire.core :as cheshire]
            [ring.util.http-response :as resp]))

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
      (content-type
        (assoc response
          :body (cheshire/encode exception)
          :throwable error
          :exception exception)
        "application/json; charset=utf-8"))))

(defn invalid-cfg-handler
  [_ error-type _]
  (resp/internal-server-error {:errors (:reason error-type)}))

(defn unchecked-handler
  [error error-type _]
  (cond
    (ec/error? error-type) (assoc (resp/internal-server-error (cheshire/encode error-type))
                             :throwable error
                             :exception error-type)

    (instance? Object error) (let [exception {:error_code ec/ERR_UNCHECKED_EXCEPTION
                                              :reason (:message error-type)}]
                               (assoc (resp/internal-server-error (cheshire/encode exception))
                                 :throwable error
                                 :exception exception))))







