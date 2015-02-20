(ns metadactyl.service.oauth
  "Service implementations dealing with OAuth 2.0 authentication."
  (:use [metadactyl.user :only [current-user]]
        [slingshot.slingshot :only [throw+]])
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.oauth :as op]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defn- build-authy-server-info
  "Builds the server info to pass to authy."
  [server-info token-callback]
  (assoc (dissoc server-info :api-name)
    :token-callback token-callback))

(def ^:private server-info-fn-for
  {:agave config/agave-oauth-settings})

(defn- get-server-info
  "Retrieves the server info for the given API name."
  [api-name]
  (if-let [server-info-fn (server-info-fn-for (keyword api-name))]
    (server-info-fn)
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     (str "unknown API name: " api-name)})))

(defn get-access-token
  "Receives an OAuth authorization code and obtains an access token."
  [api-name {:keys [code state]}]
  (let [server-info    (get-server-info api-name)
        username       (:username current-user)
        state-info     (op/retrieve-authorization-request-state state username)
        token-callback (partial op/store-access-token api-name username)]
    (authy/get-access-token (build-authy-server-info server-info token-callback) code)
    (service/success-response {:state_info state-info})))
