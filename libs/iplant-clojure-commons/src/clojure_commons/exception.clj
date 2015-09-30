(ns clojure-commons.exception
  (:use [slingshot.slingshot :only [get-throw-context]])
  (:require [clojure-commons.error-codes :as ec]
            [cheshire.core :as cheshire]
            [compojure.api.exception :as ex]
            [ring.util.response :as header]
            [ring.util.http-response :as resp]))

(defn- embed-error-info
  [throwable exception response]
  (assoc response
    :throwable throwable
    :exception exception))

(def ^:private clj-http-error?
  (every-pred :status :headers :body))

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
        (embed-error-info error
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
        (embed-error-info error
                          exception
                          (resp/unauthorized (cheshire/encode exception)))
        "WWW-Authenticate"
        "Custom"))))

(defn invalid-cfg-handler
  [error error-type _]
  (let [exception {:error_code ec/ERR_CONFIG_INVALID
                   :reason (:error error-type)}]
    (embed-error-info error
                      exception
                      (resp/internal-server-error (cheshire/encode exception)))))

(defn missing-request-field-handler
  [error error-type _]
  (let [exception {:error_code ec/ERR_BAD_OR_MISSING_FIELD
                   :fields (:fields error-type)}]
    (embed-error-info error
                      exception
                      (resp/bad-request (cheshire/encode exception)))))

(defn bad-request-field-handler
  [error error-type _]
  (missing-request-field-handler error error-type _))

(defn missing-query-params-handler
  [error error-type _]
  (let [exception {:error_code ec/ERR_MISSING_QUERY_PARAMETER
                   :parameters (:parameters error-type)}]
    (embed-error-info error
                      exception
                      (resp/bad-request (cheshire/encode exception)))))

(defn bad-query-params-handler
  [error error-type _]
  (let [exception {:error_code ec/ERR_BAD_QUERY_PARAMETER
                   :parameters (:parameters error-type)}]
    (embed-error-info error
                      exception
                      (resp/bad-request (cheshire/encode exception)))))

(defn unchecked-handler
  [error error-type _]
  (let [error-obj (:object (get-throw-context error))]
    (cond
     (ec/error? error-obj)
     (embed-error-info error
                       error-obj
                       (resp/internal-server-error (cheshire/encode error-obj)))

     (clj-http-error? error-obj)
     (embed-error-info error
                       {:error_code ec/ERR_REQUEST_FAILED}
                       error-obj)

     (instance? Object error)
     (let [exception {:error_code ec/ERR_UNCHECKED_EXCEPTION
                      :reason (:message error-type)}]
       (embed-error-info error
                         exception
                         (resp/internal-server-error (cheshire/encode exception)))))))

(def handle-request-validation-errors
  (as-de-exception-handler ex/request-validation-handler ec/ERR_ILLEGAL_ARGUMENT))

(def handle-response-validation-errors
  (as-de-exception-handler ex/response-validation-handler ec/ERR_SCHEMA_VALIDATION))

(def exception-handlers
  {:handlers
   {::ex/request-validation    handle-request-validation-errors
    ::ex/response-validation   handle-response-validation-errors
    ::invalid-cfg              invalid-cfg-handler
    ::authentication-not-found authentication-not-found-handler
    ::missing-request-field    missing-request-field-handler
    ::bad-request-field        bad-request-field-handler
    ::missing-query-params     missing-query-params-handler
    ::bad-query-params         bad-query-params-handler
    ::ex/default               unchecked-handler}})
