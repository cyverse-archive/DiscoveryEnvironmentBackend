(ns donkey.services.oauth
  "Service implementations dealing with OAuth 2.0 authentication."
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [donkey.persistence.oauth :as op]
            [donkey.util.service :as service]))

(defn- build-authy-server-info
  "Builds the server info to pass to authy."
  [server-info token-callback]
  (assoc (dissoc server-info :api-name)
    :token-callback token-callback))

(defn get-access-token
  "Receives an OAuth authorization code and obtains an access token."
  [{:keys [api-name] :as server-info} {code :code}]
  (let [token-callback (partial op/store-access-token api-name (:username current-user))]
    (authy/get-access-token (build-authy-server-info server-info token-callback) code)
    (service/success-response)))
