(ns donkey.services.metadata.apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user with-directory-user]]
        [kameleon.uuids :only [is-uuid? uuidify]]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.persistence.jobs :as jp]
            [donkey.persistence.oauth :as op]
            [donkey.services.metadata.internal-jobs :as internal-jobs]
            [donkey.services.metadata.util :as mu]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.service :as service]
            [kameleon.db :as kdb]
            [mescal.de :as agave])
  (:import [java.util UUID]))

(defn- get-first-job-step
  [{:keys [id]}]
  (service/assert-found (jp/get-job-step-number id 1) "first step in job" id))

(defn- agave-authorization-uri
  [state-info]
  (let [username (:username current-user)
        state    (op/store-authorization-request username state-info)]
    (-> (curl/url (config/agave-oauth-base) "authorize")
        (assoc :query {:response_type "code"
                       :client_id     (config/agave-key)
                       :redirect-uri  (config/agave-redirect-uri)
                       :state         state})
        (str))))

(defn- agave-authorization-redirect
  [state-info]
  (throw+ {:error_code ce/ERR_TEMPORARILY_MOVED
           :location   (agave-authorization-uri state-info)}))

(defn- add-predicate
  [predicate f]
  (fn [& args]
    (when (predicate)
      (apply f args))))

(defprotocol AppLister
  "Used to list apps available to the Discovery Environment."
  (listAppGroups [_ params])
  (listApps [_ category-id params])
  (searchApps [_ search-term])
  (addFavoriteApp [_ app-id])
  (removeFavoriteApp [_ app-id])
  (rateApp [_ app-id rating comment-id])
  (deleteRating [_ app-id])
  (getApp [_ app-id])
  (getAppDeployedComponents [_ app-id])
  (getAppDetails [_ app-id])
  (getAppDocs [_ app-id])
  (addAppDocs [_ app-id docs])
  (editAppDocs [_ app-id docs])
  (adminAddAppDocs [_ app-id docs])
  (adminEditAppDocs [_ app-id docs])
  (listAppTasks [_ app-id])
  (editWorkflow [_ app-id])
  (copyWorkflow [_ app-id])
  (createPipeline [_ pipeline])
  (updatePipeline [_ app-id pipeline])
  (submitJob [_ submission])
  (countJobs [_ filter include-hidden])
  (listJobs [_ limit offset sort-field sort-order filter include-hidden])
  (syncJobStatus [_ job])
  (updateJobStatus [_ username job job-step status end-time])
  (updateBatchStatus [_ batch completion-date])
  (stopJob [_ job])
  (getJobParams [_ job-id])
  (getAppRerunInfo [_ job-id])
  (urlImport [_ address filename dest-path]))
;; AppLister

(deftype DeOnlyAppLister []
  AppLister

  (getAppDocs [_ app-id]
    (metadactyl/get-app-docs app-id))

  (addAppDocs [_ app-id docs]
    (metadactyl/add-app-docs app-id docs))

  (editAppDocs [_ app-id docs]
    (metadactyl/edit-app-docs app-id docs))

  (adminAddAppDocs [_ app-id docs]
    (metadactyl/admin-add-app-docs app-id docs))

  (adminEditAppDocs [_ app-id docs]
    (metadactyl/admin-edit-app-docs app-id docs)))
;; DeOnlyAppLister

(deftype DeHpcAppLister [agave-client user-has-access-token?]
  AppLister

  (getAppDocs [_ app-id]
    (if (is-uuid? app-id)
      (metadactyl/get-app-docs app-id)
      {:app_id        app-id
       :documentation ""
       :references    []}))

  (addAppDocs [_ app-id docs]
    (if (is-uuid? app-id)
      (metadactyl/add-app-docs app-id docs)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "Cannot edit documentation for HPC apps with this service"})))

  (editAppDocs [_ app-id docs]
    (if (is-uuid? app-id)
      (metadactyl/edit-app-docs app-id docs)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "Cannot edit documentation for HPC apps with this service"})))

  (adminAddAppDocs [_ app-id docs]
    (if (is-uuid? app-id)
      (metadactyl/admin-add-app-docs app-id docs)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "Cannot edit documentation for HPC apps with this service"})))

  (adminEditAppDocs [_ app-id docs]
    (if (is-uuid? app-id)
      (metadactyl/admin-edit-app-docs app-id docs)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "Cannot edit documentation for HPC apps with this service"}))))
;; DeHpcAppLister

(defn- has-access-token
  [{:keys [api-name] :as server-info} username]
  (seq (op/get-access-token api-name username)))

(defn- get-access-token
  [{:keys [api-name] :as server-info} state-info username]
  (if-let [token-info (op/get-access-token api-name username)]
    (assoc (merge server-info token-info)
      :token-callback  (partial op/store-access-token api-name username)
      :reauth-callback (partial agave-authorization-redirect state-info))
    (agave-authorization-redirect state-info)))

(defn- get-agave-client
  [state-info username]
  (agave/de-agave-client-v2
   (config/agave-base-url)
   (config/agave-storage-system)
   (partial get-access-token (config/agave-oauth-settings) state-info username)
   (config/agave-jobs-enabled)))

(defn- get-de-hpc-app-lister
  [state-info username]
  (DeHpcAppLister. (get-agave-client state-info username)
                   (partial has-access-token (config/agave-oauth-settings) username)))

(defn- get-app-lister
  ([]
     (get-app-lister ""))
  ([state-info]
     (get-app-lister state-info (:username current-user)))
  ([state-info username]
     (if (config/agave-enabled)
       (get-de-hpc-app-lister state-info username)
       (DeOnlyAppLister.))))

(defn get-app-docs
  [app-id]
  (service/success-response
    (.getAppDocs (get-app-lister) app-id)))

(defn add-app-docs
  [app-id body]
  (service/success-response
    (.addAppDocs (get-app-lister) app-id (service/decode-json body))))

(defn edit-app-docs
  [app-id body]
  (service/success-response
    (.editAppDocs (get-app-lister) app-id (service/decode-json body))))

(defn admin-add-app-docs
  [app-id body]
  (service/success-response
    (.adminAddAppDocs (get-app-lister) app-id (service/decode-json body))))

(defn admin-edit-app-docs
  [app-id body]
  (service/success-response
    (.adminEditAppDocs (get-app-lister) app-id (service/decode-json body))))
