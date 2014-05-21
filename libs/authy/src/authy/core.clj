(ns authy.core
  (:require [clj-http.client :as http])
  (:import [java.util Date]))

(defn- call-token-callback
  [{:keys [token-callback] :as token-info}]
  (when (fn? token-callback)
    (token-callback token-info))
  token-info)

(defn- format-token-info
  [token-info]
  (assoc (dissoc token-info :token_type :expires_in :refresh_token :access_token)
    :token-type    (:token_type token-info)
    :expires-at    (Date. (+ (System/currentTimeMillis) (* 1000 (:expires_in token-info))))
    :refresh-token (:refresh_token token-info)
    :access-token  (:access_token token-info)))

(defn- auth-code-token-request
  [{:keys [token-uri redirect-uri client-key client-secret]} code]
  (:body (http/post token-uri
                    {:basic-auth  [client-key client-secret]
                     :form-params {:grant_type   "authorization_code"
                                   :code         code
                                   :redirect_uri redirect-uri}
                     :as          :json})))

(defn get-access-token
  [oauth-info code]
  (->> (auth-code-token-request oauth-info code)
       (format-token-info)
       (merge oauth-info)
       (call-token-callback)))

(defn- refresh-token-request
  [{:keys [token-uri client-key client-secret refresh-token]}]
  (:body (http/post token-uri
                    :basic-auth  [client-key client-secret]
                    :form-params {:grant_type    "refresh_token"
                                  :refresh_token refresh-token}
                    :as          :json)))

(defn refresh-access-token
  [token-info]
  (->> (refresh-token-request token-info)
       (format-token-info)
       (merge token-info)
       (call-token-callback)))

(defn token-expired?
  [{:keys [expires-at]}]
  (neg? (.compareTo (Date.) expires-at)))
