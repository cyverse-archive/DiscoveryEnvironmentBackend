(ns data-info.util.service
  (:use [cemerick.url :only [url]]
        [ring.util.codec :only [url-encode]]
        [clojure.java.io :only [reader]]
        [clojure.string :only [join blank?] :as string]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [data-info.util.config :as config]
            [ring.util.codec :as codec])
  (:import [clojure.lang IPersistentMap]))

(defn empty-response []
  {:status 200})

(defn error-body [e]
  (cheshire/encode {:success false :reason (.getMessage e)}))

(defn success?
  "Returns true if status-code is between 200 and 299, inclusive."
  [status-code]
  (<= 200 status-code 299))

(defn response-map?
  "Returns true if 'm' can be used as a response map. We're defining a
   response map as a map that contains a :status and :body field."
  [m]
  (and (map? m)
       (contains? m :status)
       (contains? m :body)))

(def ^:private default-content-type
  "application/json; charset=utf-8")

(defn- content-type-specified?
  [e]
  (or (contains? e :content-type)
      (contains? (:headers e) "Content-Type")))

(defn- data-info-response-from-response-map
  [e status-code]
  (if-not (content-type-specified? e)
    (update-in e [:headers] assoc "Content-Type" default-content-type)
    e))

(defn- data-info-response-from-map
  [e status-code]
  {:status  status-code
   :body    (cheshire/encode (assoc e :success (success? status-code)))
   :headers {"Content-Type" default-content-type}})

(defn- error-resp?
  [e status-code]
  (and (instance? Exception e)
       (not (success? status-code))))

(defn- data-info-response-from-exception
  [e status-code]
  {:status  status-code
   :body    (error-body e)
   :headers {"Content-Type" default-content-type}})

(defn- default-data-info-response
  [e status-code]
  {:status status-code
   :body   e})

(defn data-info-response
  "Generates a data-info HTTP response map based on a value and a status code.

   If a response map is passed in, it is preserved.

   If a response map is passed in and is missing the content-type field,
   then the content-type is set to application/json.

   If it's a map but not a response map, then the :success field is merged in,
   then is JSON encoded, and it is finally used as the body of the response.

   Otherwise, the value is preserved and is wrapped in a response map."
  [e status-code]
  (cond
   (response-map? e)           (data-info-response-from-response-map e status-code)
   (map? e)                    (data-info-response-from-map e status-code)
   (error-resp? e status-code) (data-info-response-from-exception e status-code)
   :else                       (default-data-info-response e status-code)))

(defn success-response
  ([]
     (success-response {}))
  ([retval]
    (data-info-response retval 200)))

(defn create-response
  "Generates a 201 response indicating that a new resource has been created. Optionally, a JSON
   document may be included and will form part of the response body."
  ([]
    (create-response {}))
  ([retval]
    (data-info-response retval 201)))

(defn failure-response [e]
  (log/error e "bad request")
  (data-info-response e 400))

(defn error-response
  ([e]
    (error-response e 500))
  ([e status]
    (when (>= status 500) (log/error e "internal error"))
    (data-info-response e status)))

(defn temp-dir-failure-response [{:keys [parent prefix base]}]
  (log/error "unable to create a temporary directory in" parent
             "using base name" base)
  {:status       500
   :content-type :json
   :body         (cheshire/encode {:success    false
                                   :error_code "ERR-TEMP-DIR-CREATION"
                                   :parent     parent
                                   :prefix     prefix
                                   :base       base})})

(defn tree-file-parse-err-response [{:keys [details]}]
  {:status       400
   :content-type :json
   :body         (cheshire/encode {:success    false
                                   :error_code "ERR-TREE-FILE-PARSE"
                                   :details    details})})

(defn common-error-code [exception]
  (log/error ce/format-exception exception)
  (ce/err-resp (:object exception)))

(def ^:private param-type-descriptions
  {ce/ERR_MISSING_FORM_FIELD      "request body field"
   ce/ERR_MISSING_QUERY_PARAMETER "query string parameter"})

(defn- required-argument-missing-reason
  [err-code k]
  (str "required " (param-type-descriptions err-code) ", " (name k) ", missing"))

(defn- required-argument
  [err-code m k]
  (let [v (m k)]
    (when (or (nil? v) (and (string? v) (blank? v)))
      (throw+ {:error_code err-code
               :reason     (required-argument-missing-reason err-code k)}))
    v))

(def required-param (partial required-argument ce/ERR_MISSING_QUERY_PARAMETER))
(def required-field (partial required-argument ce/ERR_MISSING_FORM_FIELD))

(defn unrecognized-path-response
  "Builds the response to send for an unrecognized service path."
  []
  (let [msg "unrecognized service path"]
    (cheshire/encode {:success false :reason msg})))

(defn prepare-forwarded-request
  "Prepares a request to be forwarded to a remote service."
  ([request body]
    {:content-type (or (get-in request [:headers :content-type])
                       (get-in request [:content-type]))
      :headers (dissoc
                (:headers request)
                "content-length"
                "content-type"
                "transfer-encoding")
      :body body
      :throw-exceptions false
      :as :stream})
  ([request]
     (prepare-forwarded-request request nil)))

(defn forward-get
  "Forwards a GET request to a remote service.  If no body is provided, the
   request body is stripped off.

   Parameters:
     addr - the URL receiving the request
     request - the request to send structured by compojure
     body - the body to attach to the request

   Returns:
     the response from the remote service"
  ([addr request]
     (client/get addr (prepare-forwarded-request request)))
  ([addr request body]
     (client/get addr (prepare-forwarded-request request body))))

(defn forward-post
  "Forwards a POST request to a remote service."
  ([addr request]
     (forward-post addr request (slurp (:body request))))
  ([addr request body]
     (client/post addr (prepare-forwarded-request request body))))

(defn forward-put
  "Forwards a PUT request to a remote service."
  ([addr request]
     (forward-put addr request (slurp (:body request))))
  ([addr request body]
     (client/put addr (prepare-forwarded-request request body))))

(defn forward-patch
  "Forwards a PATCH request to a remote service."
  ([addr request]
   (forward-patch addr request (slurp (:body request))))
  ([addr request body]
   (client/patch addr (prepare-forwarded-request request body))))

(defn forward-delete
  "Forwards a DELETE request to a remote service."
  [addr request]
  (client/delete addr (prepare-forwarded-request request)))

(defn decode-stream
  "Decodes a stream containing a JSON object."
  [stream]
  (cheshire/decode-stream (reader stream) true))

(defn decode-json
  "Decodes JSON from either a string or an input stream."
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))

(defn- contains-form?
  "Determines if a request contains a URL encoded form."
  [req]
  (re-find #"^application/x-www-form-urlencoded" (str (:content-type req))))

(defn parse-form
  "Parses a URL encoded form from a request."
  [req]
  (or (if-let [body (and (contains-form? req) (:body req))]
        (let [encoding (or (:character-encoding req) "UTF-8")
              content  (slurp body :encoding encoding)
              params   (codec/form-decode content encoding)]
          (when (map? params) params)))
      {}))

(defmacro log-runtime
  [[msg] & body]
  `(let [start#  (System/currentTimeMillis)
         result# (do ~@body)
         finish# (System/currentTimeMillis)]
     (when (config/log-runtimes)
       (log/warn ~msg "-" (- finish# start#) "milliseconds"))
     result#))

(defn not-found
  "Throws an exception indicating that an object wasn't found."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_FOUND
           :reason     (string/join " " [desc id "not found"])}))

(defn not-owner
  "Throws an exception indicating that the user isn't permitted to perform the requested option."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_OWNER
           :reason     (str "authenticated user doesn't own " desc ", " id)}))

(defn not-unique
  "Throws an exception indicating that multiple objects were found when only one was expected."
  [desc id]
  (throw+ {:error_code ce/ERR_NOT_UNIQUE
           :reason     (string/join " " [desc id "not unique"])}))

(defn bad-request
  "Throws an exception indicating that the incoming request is invalid."
  [reason]
  (throw+ {:error_code ce/ERR_BAD_REQUEST
           :reason     reason}))

(defn assert-found
  "Asserts that an object to modify or retrieve was found."
  [obj desc id]
  (if (nil? obj)
    (not-found desc id)
    obj))

(defn assert-valid
  "Throws an exception if an arbitrary expression is false."
  [valid? & msgs]
  (when-not valid?
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message    (string/join " " msgs)})))

(defn string->long
  "Converts a String to a long."
  [string & msgs]
  (try
    (Long/parseLong string)
    (catch NumberFormatException e
           (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
                    :message    (string/join " " msgs)}))))

(defn request-failure
  "Throws an exception indicating that a request failed for an unexpected reason."
  [& msgs]
  (throw+ {:error_code ce/ERR_REQUEST_FAILED
           :message    (string/join " " msgs)}))

(defn- success-response*
  [body]
  {:status  200
   :body    body
   :headers {"Content-Type" default-content-type}})

(defn trap
  "Traps a service call, automatically calling success-response on the result."
  [action func & args]
  (ce/trap action #(success-response* (apply func args))))
