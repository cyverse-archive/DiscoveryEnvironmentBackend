(ns mescal.agave-v1
  (:use [clojure.java.io :only [reader]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [cemerick.url :as curl]
            [clj-http.client :as client]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log])
  (:import [java.io IOException]))

(defn- decode-json
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))

(defn- extract-result
  [response default]
  (letfn [(get-result [m] (:result m default))]
    ((comp get-result decode-json :body) response)))

(defn authenticate
  [base-url proxy-user proxy-pass user timeout]
  ((comp :token :result decode-json :body)
   (try+
    (client/post (str (curl/url base-url "auth-v1") "/")
                 {:accept         :json
                  :as             :stream
                  :form-params    {:username user}
                  :basic-auth     [proxy-user proxy-pass]
                  :conn-timeout   timeout
                  :socket-timeout timeout})
    (catch IOException e
      (let [msg "the Foundation API appears to be unavailable at this time"]
        (log/error e msg)
        (throw+ {:error_code ce/ERR_UNAVAILABLE
                 :reason     msg})))
    (catch Object e
      (let [msg "unable to authenticate to the Foundation API"]
        (log/error e msg)
        (throw+ {:error_code ce/ERR_UNCHECKED_EXCEPTION
                 :reason     msg}))))))

(defn list-systems
  [base-url]
  (extract-result
   (client/get (str (curl/url base-url "apps-v1" "systems" "list"))
               {:accept :json
                :as     :stream})
   []))

(defn list-public-apps
  [base-url]
  (extract-result
   (client/get (str (curl/url base-url "apps-v1" "apps" "list"))
               {:accept :json
                :as     :stream})
   []))

(defn list-my-apps
  [base-url user token]
  (extract-result
   (client/get (str (curl/url base-url "apps-v1" "apps" "share" "list"))
               {:basic-auth [user token]
                :accept     :json
                :as         :stream})
   []))

(defn get-app
  [base-url app-id]
  (extract-result
   (client/get (str (curl/url base-url "apps-v1" "apps" app-id))
               {:accept :json
                :as     :stream})
   {}))

(defn- required-field-missing
  [field-name]
  (throw (IllegalArgumentException. (str "Missing required field, " field-name))))

(defn- required-field
  [field m]
  (let [v (m field)]
    (when (nil? v)
      (required-field-missing (name field)))
    v))

(defn- common-params
  [params]
  {:softwareName   (required-field :softwareName params)
   :jobName        (required-field :jobName params)
   :processorCount (:processorCount params)
   :maxMemory      (:maxMemory params)
   :requestedTime  (required-field :requestedTime params)
   :callbackUrl    (:callbackUrl params)
   :archive        (:archive params)
   :archivePath    (:archivePath params)})

(defn- param-value
  [params param-def]
  (let [{id :id {:keys [required default]} :value} param-def
        field-name                                 (keyword id)
        field-value                                (or (field-name params) default)]
    (when (and required (nil? field-value))
      (required-field-missing (name field-name)))
    [field-name field-value]))

(defn- params
  [param-defs params]
  (into {} (map (partial param-value params) param-defs)))

(defn submit-job
  ([base-url user token params]
     (submit-job base-url user token (get-app base-url (:softwareName params)) params))
  ([base-url user token app params]
     (let [form (common-params params)
           form (merge form (params (:inputs app) params))
           form (merge form (params (:parameters app) params))
           form (into {} (remove (fn [[_ v]] (nil? v)) form))]
       (extract-result
        (client/post (str (curl/url base-url "apps-v1" "job") "/")
                     {:basic-auth  [user token]
                      :form-params form
                      :accept      :json
                      :as          :stream})
        {}))))

(defn list-job
  [base-url user token job-id]
  (extract-result
   (client/get (str (curl/url base-url "apps-v1" "job" job-id))
               {:basic-auth [user token]
                :accept     :json
                :as         :stream})
   {}))

(defn list-jobs
  ([base-url user token]
     (extract-result
      (client/get (str (curl/url base-url "apps-v1" "jobs" "list"))
                  {:basic-auth [user token]
                   :accept     :json
                   :as         :stream})
      []))
  ([base-url user token job-ids]
     (let [job-ids (map str job-ids)]
       (filter (comp (set job-ids) str :id)
               (list-jobs base-url user token)))))
