(ns mescal.agave-v2
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [mescal.util :as util]
            [clojure.tools.logging :as log]))

(defn- refresh-access-token
  [token-info-fn timeout]
  (let [new-token-info (authy/refresh-access-token @(token-info-fn) :timeout timeout)]
    (dosync (ref-set (token-info-fn) new-token-info))))

(defn- wrap-refresh
  [token-info-fn timeout request-fn]
  (try+
   (request-fn)
   (catch [:status 401] _
     (refresh-access-token token-info-fn timeout)
     (request-fn))))

(defmacro ^:private with-refresh
  [[token-info-fn timeout] & body]
  `(wrap-refresh ~token-info-fn ~timeout #(do ~@body)))

(defn check-access-token
  [token-info-fn timeout]
  (when (authy/token-expiring? @(token-info-fn))
    (refresh-access-token token-info-fn timeout)))

(defn agave-get
  [token-info-fn timeout url]
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/get (str url)
               {:oauth-token    (:access-token @(token-info-fn))
                :as             :json
                :conn-timeout   timeout
                :socket-timeout timeout}))))

(defn agave-post
  [token-info-fn timeout url body]
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/post (str url)
                {:oauth-token   (:access-token @(token-info-fn))
                 :as            :json
                 :accept        :json
                 :content-type  :json
                 :form-params   body}))))

(defn list-systems
  [base-url token-info-fn timeout]
  (agave-get token-info-fn timeout (curl/url base-url "/systems/v2/")))

(defn list-apps
  [base-url token-info-fn timeout]
  (agave-get token-info-fn timeout (curl/url base-url "/apps/v2/")))

(defn get-app
  [base-url token-info-fn timeout app-id]
  (agave-get token-info-fn timeout (curl/url base-url "/apps/v2" app-id)))

(defn submit-job
  [base-url token-info-fn timeout submission]
  (agave-post token-info-fn timeout (curl/url base-url "/jobs/v2/") submission))
