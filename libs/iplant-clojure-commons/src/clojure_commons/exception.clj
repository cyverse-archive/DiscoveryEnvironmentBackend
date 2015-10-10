(ns clojure-commons.exception
  (:use [slingshot.slingshot :only [get-throw-context]]
        [service-logging.thread-context :only [with-logging-context]])
  (:require [clojure-commons.error-codes :as ec]
            [cheshire.core :as cheshire]
            [compojure.api.exception :as ex]
            [ring.util.response :as header]
            [service-logging.middleware :as smw]
            [ring.util.http-response :as resp]))

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
    (let [hndlr-resp (error-handler error error-type request)
          exception {:error_code error-code
                     :reason (get-in hndlr-resp [:body :errors])}
          response (-> hndlr-resp
                       (assoc :body (cheshire/encode exception))
                       (header/content-type "application/json; charset=utf-8"))]
      (smw/log-response-with-exception error
                                       request
                                       response
                                       exception)
      response)))

(defn- exception-with-reason
  ([error-code reason]
   (exception-with-reason error-code reason {}))
  ([error-code reason m]
   (assoc m
     :error_code error-code
     :reason     reason)))

(defn- apply-headers
  [response headers-map]
  (reduce (fn [response [header-name header-value]]
            (header/header response header-name header-value))
          response headers-map))

(defn default-error-handler
  "Produces an error handler which generates exceptions with the
  given error-code and HTTP response function."
  [response-fn error-code & [header-map]]
  (fn [error error-type request]
    (let [reason    (:error error-type)
          exception (exception-with-reason error-code reason (dissoc error-type :type :error))
          header-map (assoc header-map "Content-Type" "application/json; charset=utf-8")
          response  (apply-headers (response-fn (cheshire/encode exception)) header-map)]
      (smw/log-response-with-exception error request response exception)
      response)))

(defn temporary-redirect-handler
  "Specialized exception handler for 302 responses. This could not be accomplished with the default
   exception handler because best practices for 302 responses is an empty response body."
  [error error-type request]
  (let [reason (:error error-type)
        exception (exception-with-reason ec/ERR_TEMPORARILY_MOVED reason (dissoc error-type :type :error))
        location (:location error-type)
        response (resp/found location)]
    (smw/log-response-with-exception error request response exception)
    response))

(def authentication-not-found-handler
  (default-error-handler resp/unauthorized ec/ERR_NOT_AUTHORIZED {"WWW-Authenticate" "Custom"}))

(def bad-query-params-handler
  (default-error-handler resp/bad-request ec/ERR_BAD_QUERY_PARAMETER))

(def bad-request-field-handler
  (default-error-handler resp/bad-request ec/ERR_BAD_OR_MISSING_FIELD))

(def failed-dependency-handler
  (default-error-handler resp/failed-dependency ec/ERR_MISSING_DEPENDENCY))

(def forbidden-handler
  (default-error-handler resp/forbidden ec/ERR_FORBIDDEN {"WWW-Authenticate" "Custom"}))

(def handle-request-validation-errors
  (as-de-exception-handler ex/request-validation-handler ec/ERR_ILLEGAL_ARGUMENT))

(def handle-response-validation-errors
  (as-de-exception-handler ex/response-validation-handler ec/ERR_SCHEMA_VALIDATION))

(def illegal-argument-handler
  (default-error-handler resp/bad-request ec/ERR_ILLEGAL_ARGUMENT))

(def invalid-cfg-handler
  (default-error-handler resp/internal-server-error ec/ERR_CONFIG_INVALID))

(def invalid-json-handler
  (default-error-handler resp/bad-request ec/ERR_INVALID_JSON))

(def item-exists-handler
  (default-error-handler resp/bad-request ec/ERR_EXISTS))

(def missing-query-params-handler
  (default-error-handler resp/bad-request ec/ERR_MISSING_QUERY_PARAMETER))

(def missing-request-field-handler
  (default-error-handler resp/bad-request ec/ERR_BAD_OR_MISSING_FIELD))

(def not-authorized-handler
  (default-error-handler resp/unauthorized ec/ERR_NOT_AUTHORIZED))

(def not-found-handler
  (default-error-handler resp/not-found ec/ERR_NOT_FOUND))

(def not-owner-handler
  (default-error-handler resp/forbidden ec/ERR_NOT_OWNER))

(def not-writeable-handler
  (default-error-handler resp/bad-request ec/ERR_NOT_WRITEABLE))

(def not-a-user-handler
  (default-error-handler resp/not-found ec/ERR_NOT_A_USER))

(def not-unique-handler
  (default-error-handler resp/bad-request ec/ERR_NOT_UNIQUE))

(def request-failed-handler
  (default-error-handler resp/internal-server-error ec/ERR_REQUEST_FAILED))

(def unavailable-handler
  (default-error-handler resp/gateway-timeout ec/ERR_UNAVAILABLE))

(defn unchecked-handler
  [error error-type request]
  (let [error-obj (:object (get-throw-context error))]
    (cond
      (ec/error? error-obj)
      (let [exception error-obj
            response (resp/internal-server-error (cheshire/encode error-obj))]
        (smw/log-response-with-exception error
                                         request
                                         response
                                         exception)
        response)

      (clj-http-error? error-obj)
      (let [exception {:error_code ec/ERR_REQUEST_FAILED}
            response error-obj]
        (smw/log-response-with-exception error
                                         request
                                         response
                                         exception)
        response)

      (instance? IllegalArgumentException error)
      (let [exception {:error_code ec/ERR_ILLEGAL_ARGUMENT
                       :reason (.toString error)}
            response (resp/bad-request (cheshire/encode exception))]
        (smw/log-response-with-exception error
                                         request
                                         response
                                         exception)
        response)

      (instance? Exception error)
      (let [exception {:error_code ec/ERR_UNCHECKED_EXCEPTION
                       :reason (.toString error)}
            response (resp/internal-server-error (cheshire/encode exception))]
        (smw/log-response-with-exception error
                                         request
                                         response
                                         exception)
        response)

      (instance? Object error)
      (let [exception {:error_code ec/ERR_UNCHECKED_EXCEPTION
                       :reason (:message error-type)}
            response (resp/internal-server-error (cheshire/encode exception))]
        (smw/log-response-with-exception error
                                         request
                                         response
                                         exception)
        response))))

(def exception-handlers
  {:handlers
   {::ex/request-validation    handle-request-validation-errors
    ::ex/response-validation   handle-response-validation-errors
    ::exists                   item-exists-handler
    ::failed-dependency        failed-dependency-handler
    ::illegal-argument         illegal-argument-handler
    ::not-found                not-found-handler
    ::not-unique               not-unique-handler
    ::invalid-cfg              invalid-cfg-handler
    ::invalid-json             invalid-json-handler
    ::authentication-not-found authentication-not-found-handler
    ::not-authorized           not-authorized-handler
    ::not-a-user               not-a-user-handler
    ::not-owner                not-owner-handler
    ::not-writeable            not-writeable-handler
    ::forbidden                forbidden-handler
    ::missing-request-field    missing-request-field-handler
    ::bad-request-field        bad-request-field-handler
    ::missing-query-params     missing-query-params-handler
    ::bad-query-params         bad-query-params-handler
    ::request-failed           request-failed-handler
    ::temporary-redirect       temporary-redirect-handler
    ::unavailable              unavailable-handler
    ::ex/default               unchecked-handler}})


