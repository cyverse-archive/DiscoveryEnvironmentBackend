(ns clojure-commons.error-codes
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]
            [clojure.string :as string])
  (:import [java.io PrintWriter
                    StringWriter]))

(defmacro deferr
  [sym]
  `(def ~sym (name (quote ~sym))))

(deferr ERR_DOES_NOT_EXIST)
(deferr ERR_EXISTS)
(deferr ERR_NOT_WRITEABLE)
(deferr ERR_NOT_READABLE)
(deferr ERR_WRITEABLE)
(deferr ERR_READABLE)
(deferr ERR_NOT_A_USER)
(deferr ERR_NOT_A_FILE)
(deferr ERR_NOT_A_FOLDER)
(deferr ERR_IS_A_FILE)
(deferr ERR_IS_A_FOLDER)
(deferr ERR_INVALID_JSON)
(deferr ERR_BAD_OR_MISSING_FIELD)
(deferr ERR_NOT_AUTHORIZED)
(deferr ERR_MISSING_QUERY_PARAMETER)
(deferr ERR_MISSING_FORM_FIELD)
(deferr ERR_BAD_QUERY_PARAMETER)
(deferr ERR_INCOMPLETE_DELETION)
(deferr ERR_INCOMPLETE_MOVE)
(deferr ERR_INCOMPLETE_RENAME)
(deferr ERR_REQUEST_FAILED)
(deferr ERR_UNCHECKED_EXCEPTION)
(deferr ERR_NOT_OWNER)
(deferr ERR_FORBIDDEN)
(deferr ERR_INVALID_COPY)
(deferr ERR_INVALID_URL)
(deferr ERR_TICKET_EXISTS)
(deferr ERR_TICKET_DOES_NOT_EXIST)
(deferr ERR_MISSING_DEPENDENCY)
(deferr ERR_CONFIG_INVALID)
(deferr ERR_ILLEGAL_ARGUMENT)
(deferr ERR_BAD_REQUEST)
(deferr ERR_NOT_FOUND)
(deferr ERR_NOT_UNIQUE)
(deferr ERR_UNAVAILABLE)
(deferr ERR_TOO_MANY_RESULTS)
(deferr ERR_TEMPORARILY_MOVED)
(deferr ERR_REQUEST_BODY_TOO_LARGE)
(deferr ERR_CONFLICTING_QUERY_PARAMETER_VALUES)
(deferr ERR_SCHEMA_VALIDATION)


(def ^:private http-status-for
  {ERR_BAD_OR_MISSING_FIELD               400
   ERR_ILLEGAL_ARGUMENT                   400
   ERR_INVALID_JSON                       400
   ERR_BAD_REQUEST                        400
   ERR_BAD_QUERY_PARAMETER                400
   ERR_MISSING_QUERY_PARAMETER            400
   ERR_NOT_AUTHORIZED                     401
   ERR_NOT_OWNER                          403
   ERR_FORBIDDEN                          403
   ERR_NOT_FOUND                          404
   ERR_NOT_UNIQUE                         400
   ERR_CONFLICTING_QUERY_PARAMETER_VALUES 409
   ERR_REQUEST_BODY_TOO_LARGE             413
   ERR_TEMPORARILY_MOVED                  302})

(defn get-http-status
  [err-code]
  (get http-status-for err-code 500))

(def ^:private http-header-fn-for
  {ERR_TEMPORARILY_MOVED (fn [m] {"Location" (:location m)})})

(defn- get-http-headers
  [err-obj]
  (if-let [header-fn (http-header-fn-for (:error_code err-obj))]
    (header-fn err-obj)
    {"Content-Type" "application/json; charset=utf-8"}))

(defn error?
  [obj]
  (try
    (contains? obj :error_code)
    (catch Exception _
      false)))

(defn clj-http-error?
  [{:keys [status body]}]
  (and (number? status) ((comp not nil?) body)))

(defn unchecked [throwable-map]
  {:error_code ERR_UNCHECKED_EXCEPTION
   :message (:message throwable-map)})

(defn err-resp
  ([err-obj]
     {:status (get-http-status (:error_code err-obj))
      :headers (get-http-headers err-obj)
      :body (cheshire/encode err-obj)})
  ([_ err-obj]
   (err-resp err-obj)))

(defn invalid-cfg-response
  [reason]
  (err-resp {:error_code ERR_CONFIG_INVALID
             :reason     reason}))

(defn invalid-arg-response [arg val reason]
  (err-resp {:error_code ERR_ILLEGAL_ARGUMENT
             :reason     reason
             :arg        (name arg)
             :val        val}))

(defn missing-arg-response [arg]
  (log/error "missing required argument:" (name arg))
  (err-resp {:error_code ERR_MISSING_QUERY_PARAMETER
             :arg        (name arg)}))

(defn validation-error-response [error]
  (err-resp {:error_code ERR_BAD_REQUEST
             :validation error}))

(defn- response-map?
  "Returns true if 'm' can be used as a response map. We're defining a
   response map as a map that contains a :status and :body field."
  [m]
  (and (map? m)
       (contains? m :status)
       (number? (:status m))))


(defn success-resp [_ retval]
  (log/spy :trace retval)
  (if (response-map? retval)
    retval
    {:status 200
     :body   (cond
               (map? retval)          (cheshire/encode retval)
               (not (string? retval)) (str retval)
               :else                  retval)}))


(def filters (ref #{}))

(defn register-filters
  [new-filters]
  (dosync
    (ref-set filters (set (concat @filters new-filters)))))

(defn log-filters [] @filters)

(defn format-exception
  "Formats the exception as a string."
  [^Throwable exception]
  (let [string-writer (StringWriter.)
        print-writer  (PrintWriter. string-writer)]
    (.printStackTrace exception print-writer)
    (reduce #(string/replace %1 %2 "xxxxxxxx")
      (cons (str string-writer) (log-filters)))))

(defn trap [action func & args]
  (try+
    (success-resp action (apply func args))
    (catch error? _
      (log/error (format-exception (:throwable &throw-context)))
      (err-resp action (:object &throw-context)))
    (catch clj-http-error? o o)
    (catch [:type :invalid-argument] {:keys [reason arg val]} (invalid-arg-response arg val reason))
    (catch [:type :ring.swagger.schema/validation] {:keys [error]} (validation-error-response error))
    (catch [:type :compojure.api.exception/request-validation] {:keys [error]} (validation-error-response error))
    (catch [:type :compojure.api.exception/response-validation] {:keys [error]} (validation-error-response error))
    (catch Object e
      (log/error (format-exception (:throwable &throw-context)))
      (err-resp action (unchecked &throw-context)))))

(defn wrap-errors
  "Ring handler for formatting errors that sneak by (trap). For instance
   errors that occur in other Ring handlers."
  [handler]
  (fn [req]
    (try+
      (handler req)
      (catch Exception _
        (log/error (format-exception (:throwable &throw-context)))
        (err-resp (unchecked &throw-context))))))
