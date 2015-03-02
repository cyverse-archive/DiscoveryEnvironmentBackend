(ns metadactyl.service.apps
  (:use [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [mescal.de :as agave]
            [metadactyl.persistence.oauth :as op]
            [metadactyl.service.apps.agave]
            [metadactyl.service.apps.combined]
            [metadactyl.service.apps.de]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defn- authorization-uri
  [server-info username state-info]
  (str (assoc (curl/url (:auth-uri server-info))
         :query {:response_type "code"
                 :client_id     (:client-key server-info)
                 :redirect-uri  (:redirect-uri server-info)
                 :state         (op/store-authorization-request username state-info)})))

(defn- authorization-redirect
  [server-info username state-info]
  (throw+ {:error_code ce/ERR_TEMPORARILY_MOVED
           :location   (authorization-uri server-info username state-info)}))

(defn- has-access-token
  [{:keys [api-name] :as server-info} username]
  (seq (op/get-access-token api-name username)))

(defn- get-access-token
  [{:keys [api-name] :as server-info} state-info username]
  (if-let [token-info (op/get-access-token api-name username)]
    (assoc (merge server-info token-info)
      :token-callback  (partial op/store-access-token api-name username)
      :reauth-callback (partial authorization-redirect server-info username state-info))
    (authorization-redirect server-info username state-info)))

(defn- get-agave-client
  [state-info username]
  (let [server-info (config/agave-oauth-settings)]
    (agave/de-agave-client-v2
     (config/agave-base-url)
     (config/agave-storage-system)
     (partial get-access-token (config/agave-oauth-settings) state-info username)
     (config/agave-jobs-enabled))))

(defn- get-agave-apps-client
  [state-info username]
  (metadactyl.service.apps.agave.AgaveApps.
   (get-agave-client state-info username)
   (partial has-access-token (config/agave-oauth-settings) username)))

(defn- get-apps-client-list
  [user state-info]
  (vector (metadactyl.service.apps.de.DeApps. user)
          (when (and user (config/agave-enabled))
            (get-agave-apps-client state-info (:username user)))))

(defn- get-apps-client
  ([user]
     (get-apps-client user ""))
  ([user state-info]
     (metadactyl.service.apps.combined.CombinedApps.
      (remove nil? (get-apps-client-list user state-info)))))

(defn get-app-categories
  [user params]
  (let [client (get-apps-client user "type=apps")]
    {:categories (transaction (.listAppCategories client params))}))

(defn list-apps-in-category
  [user category-id params]
  (let [state-info (str "type=apps&app-category=" category-id)
        client     (get-apps-client user state-info)]
    (.listAppsInCategory client category-id params)))

(defn search-apps
  [user {:keys [search] :as params}]
  (.searchApps (get-apps-client user "") search params))

(defn add-app
  [user app]
  (.addApp (get-apps-client user "") app))
