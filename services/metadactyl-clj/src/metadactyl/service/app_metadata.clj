(ns metadactyl.service.app-metadata
  "DE app metadata services."
  (:use [clojure.java.io :only [reader]]
        [clojure-commons.validators]
        [kameleon.app-groups :only [add-app-to-category
                                    decategorize-app
                                    get-app-subcategory-id
                                    remove-app-from-category]]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.app-validation :only [app-publishable?]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config :only [workspace-beta-app-category-id
                                       workspace-favorites-app-group-index]]
        [metadactyl.util.service :only [build-url success-response]]
        [metadactyl.validation :only [get-valid-user-id verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-metadata :as amp]
            [metadactyl.service.app-documentation :as app-docs]
            [metadactyl.translations.app-metadata :as atx]
            [metadactyl.util.config :as config]))

(defn- validate-app-existence
  "Verifies that apps exist."
  [app-id]
  (amp/get-app app-id))

(defn- validate-app-ownership
  "Verifies that a user owns an app."
  [username app-id]
  (when-not (every? (partial = username) (amp/app-accessible-by app-id))
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     (str username " does not own app " app-id)})))

(defn- validate-deletion-request
  "Validates an app deletion request."
  [req]
  (when (empty? (:app_ids req))
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     "no app identifiers provided"}))
  (when (and (nil? (:username current-user)) (not (:root_deletion_request req)))
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     "no username provided for non-root deletion request"}))
  (dorun (map validate-app-existence (:app_ids req)))
  (when-not (:root_deletion_request req)
    (dorun (map (partial validate-app-ownership (:username current-user)) (:app_ids req)))))

(defn permanently-delete-apps
  "This service removes apps from the database rather than merely marking them as deleted."
  [req]
  (validate-deletion-request req)
  (transaction
    (dorun (map amp/permanently-delete-app (:app_ids req)))
    (amp/remove-workflow-map-orphans))
  nil)

(defn preview-command-line
  "This service sends a command-line preview request to the JEX."
  [body]
  (let [jex-req (atx/template-cli-preview-req body)]
    (cheshire/decode-stream
     ((comp reader :body)
      (client/post
       (build-url (config/jex-base-url) "arg-preview")
       {:body             (cheshire/encode jex-req)
        :content-type     :json
        :as               :stream}))
     true)))

(defn get-app
  "This service obtains an app description that can be used to build a job submission form in
   the user interface."
  [app-id]
  (->> (amp/get-app app-id)
       (success-response)))
