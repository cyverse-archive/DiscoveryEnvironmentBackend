(ns mescal.agave-v2
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [mescal.util :as util]
            [clojure.tools.logging :as log]))

(defn check-access-token
  [token-info-fn timeout]
  (when (authy/token-expiring? @(token-info-fn))
    (let [new-token-info (authy/refresh-access-token @(token-info-fn) :timeout timeout)]
      (dosync (ref-set (token-info-fn) new-token-info)))))

(defn list-systems
  [base-url token-info-fn timeout]
  ((comp :result :body)
   (http/get (str (curl/url base-url "/systems/v2/"))
             {:oauth-token    (:access-token @(token-info-fn))
              :as             :json
              :conn-timeout   timeout
              :socket-timeout timeout})))

(defn list-apps
  [base-url token-info-fn timeout]
  ((comp :result :body)
   (http/get (str (curl/url base-url "/apps/v2/"))
             {:oauth-token    (:access-token @(token-info-fn))
              :as             :json
              :conn-timeout   timeout
              :socket-timeout timeout})))
