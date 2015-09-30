(ns authy.core
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [java.io IOException]
           [java.sql Timestamp]))

(def ^:private default-refresh-window (* 5 60 1000))

(defn- call-token-callback
  [{:keys [token-callback] :as token-info}]
  (when (fn? token-callback)
    (token-callback token-info))
  token-info)

(defn- format-token-info
  [token-info]
  (assoc (dissoc token-info :token_type :expires_in :refresh_token :access_token)
    :token-type    (:token_type token-info)
    :expires-at    (Timestamp. (+ (System/currentTimeMillis) (* 1000 (:expires_in token-info))))
    :refresh-token (:refresh_token token-info)
    :access-token  (:access_token token-info)))

(defn- auth-code-token-request
  [{:keys [token-uri redirect-uri client-key client-secret]} code timeout]
  (:body (http/post token-uri
                    {:basic-auth     [client-key client-secret]
                     :form-params    {:grant_type   "authorization_code"
                                      :code         code
                                      :redirect_uri redirect-uri}
                     :as             :json
                     :conn-timeout   timeout
                     :socket-timeout timeout})))

(defn get-access-token
  [oauth-info code & {:keys [timeout] :or {timeout 5000}}]
  (->> (auth-code-token-request oauth-info code timeout)
       (format-token-info)
       (merge oauth-info)
       (call-token-callback)))

(defn- credentials-token-request
  [{:keys [token-uri redirect-uri client-key client-secret]} username password timeout]
  (:body (http/post token-uri
                    {:basic-auth     [client-key client-secret]
                     :form-params    {:grant_type "client_credentials"
                                      :username   username
                                      :password   password}
                     :as             :json
                     :conn-timeout   timeout
                     :socket-timeout timeout})))

(defn get-access-token-for-credentials
  [oauth-info username password & {:keys [timeout] :or {timeout 5000}}]
  (->> (credentials-token-request oauth-info username password timeout)
       (format-token-info)
       (merge oauth-info)
       (call-token-callback)))

(defn- send-refresh-token-request
  [{:keys [token-uri client-key client-secret refresh-token]} timeout]
  (:body (http/post token-uri
                    {:basic-auth     [client-key client-secret]
                     :form-params    {:grant_type    "refresh_token"
                                      :refresh_token refresh-token}
                     :as             :json
                     :conn-timeout   timeout
                     :socket-timeout timeout})))

(defn- can-reauth?
  [{:keys [token-uri client-key client-secret]} timeout]
  (some->> (http/post token-uri
                      {:basic-auth       [client-key client-secret]
                       :form-params      {:grant_type  "password"
                                          :username    "fake_username"
                                          :password    "fake_password"}
                       :as               :json
                       :coerce           :always
                       :conn-timeout     timeout
                       :socket-timeout   timeout
                       :throw-exceptions false})
           (:body)
           (:error)
           (= "invalid_grant")))

(defn- check-reauth
  [token-info timeout]
  (when-not (can-reauth? token-info timeout)
    (throw (IOException. "reauthorization sanity check failed"))))

(defn- refresh-token-request
  [{:keys [reauth-callback] :as token-info} timeout]
  (try+ (send-refresh-token-request token-info timeout)
        (catch (comp #(<= 400 % 499) :status) e
          (check-reauth token-info timeout)
          (if (fn? reauth-callback)
            (reauth-callback)
            (throw+)))
        (catch Object e
          (log/error e)
          (throw+))))

(defn refresh-access-token
  [token-info & {:keys [timeout] :or {timeout 5000}}]
  (->> (refresh-token-request token-info timeout)
       (format-token-info)
       (merge token-info)
       (call-token-callback)))

(defn token-expiring?
  ([{:keys [refresh-window] :or {refresh-window default-refresh-window} :as token-info}]
     (token-expiring? token-info refresh-window))
  ([{:keys [^Timestamp expires-at]} window]
     (let [last-valid-time (Timestamp. (+ (System/currentTimeMillis) window))]
       (neg? (.compareTo expires-at last-valid-time)))))

(defn token-expired?
  [token-info]
  (token-expiring? token-info 0))
