(ns mescal.agave-v2
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [mescal.util :as util]))

(defn check-access-token
  [token-info-ref timeout]
  (when (authy/token-expiring? @token-info-ref)
    (let [new-token-info (authy/refresh-access-token @token-info-ref timeout)]
      (dosync (ref-set token-info-ref new-token-info)))))

(defn list-systems
  [base-url token-info-ref timeout]
  ((comp :result :body)
   (http/get (str (curl/url base-url "/systems/v2/"))
             {:oauth-token    (:access-token @token-info-ref)
              :as             :json
              :conn-timeout   timeout
              :socket-timeout timeout})))

(defn list-apps
  [base-url token-info-ref timeout]
  ((comp :result :body)
   (http/get (str (curl/url base-url "/apps/v2/"))
             {:oauth-token    (:access-token @token-info-ref)
              :as             :json
              :conn-timeout   timeout
              :socket-timeout timeout})))
