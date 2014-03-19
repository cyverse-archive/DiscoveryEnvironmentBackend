(ns metadactyl.service.app-metadata
  "DE app metadata services."
  (:use [clojure.java.io :only [reader]]
        [clojure-commons.validators]
        [metadactyl.util.service :only [build-url success-response parse-json]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]
            [metadactyl.persistence.app-metadata :as amp]
            [metadactyl.translations.app-metadata :as atx]
            [metadactyl.util.config :as config]))

(defn relabel-app
  "This service allows labels to be updated in any app, whether or not the app has been submitted
   for public use."
  [body]
  (let [req (parse-json body)]
    (transaction (amp/update-app-labels req (:hid (amp/get-app (:id req)))))
    (success-response)))

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
  (validate-map req {:analysis_ids #(and (vector? %) (every? string? %))})
  (when (empty? (:analysis_ids req))
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     "no analysis identifiers provided"}))
  (when (and (nil? (:full_username req)) (not (:root_deletion_request req)))
    (throw+ {:error_code ce/ERR_BAD_REQUEST
             :reason     "no username provided for non-root deletion request"}))
  (dorun (map validate-app-existence (:analysis_ids req)))
  (when-not (:root_deletion_request req)
    (dorun (map (partial validate-app-ownership (:full_username req)) (:analysis_ids req)))))

(defn permanently-delete-apps
  "This service removes apps from the database rather than merely marking them as deleted."
  [body]
  (let [req (parse-json body)]
    (validate-deletion-request req)
    (transaction (dorun (map amp/permanently-delete-app (:analysis_ids req)))))
  {})

(defn delete-apps
  "This service marks existing apps as deleted in the database."
  [body]
  (let [req (parse-json body)]
    (validate-deletion-request req)
    (transaction (dorun (map amp/delete-app (:analysis_ids req)))))
  {})

(defn preview-command-line
  "This service sends a command-line preview request to the JEX."
  [body]
  (let [in-req  (parse-json body)
        jex-req (atx/template-cli-preview-req in-req)]
    (cheshire/decode-stream
     ((comp reader :body)
      (client/post
       (build-url (config/jex-base-url) "arg-preview")
       {:body             (cheshire/encode jex-req)
        :content-type     :json
        :as               :stream}))
     true)))
