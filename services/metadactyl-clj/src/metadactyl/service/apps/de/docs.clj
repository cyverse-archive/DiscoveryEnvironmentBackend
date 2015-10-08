(ns metadactyl.service.apps.de.docs
  (:use [slingshot.slingshot :only [throw+]])
  (:require [metadactyl.persistence.app-documentation :as dp]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.validation :as v]))

(defn- get-references
  "Returns a list of references from the database for the given app ID."
  [app-id]
  (map :reference_text (dp/get-app-references app-id)))

(defn get-app-docs
  "Retrieves documentation details for the given app ID."
  [app-id]
  (if-let [docs (dp/get-documentation app-id)]
    (assoc docs :references (get-references app-id))
    (throw+ {:type   :clojure-commons.exception/not-found
             :error  "App documentation not found"
             :app_id app-id})))

(defn edit-app-docs
  "Updates an App's documentation and modified details in the database."
  [{:keys [username]} app-id {docs :documentation}]
  (when (get-app-docs app-id)
    (dp/edit-documentation (v/get-valid-user-id username) docs app-id))
  (get-app-docs app-id))

(defn owner-edit-app-docs
  "Updates an App's documentation in the database if the App is owned by the current user."
  [user app-id docs]
  (v/verify-app-ownership user (ap/get-app app-id))
  (edit-app-docs user app-id docs))

(defn add-app-docs
  "Adds an App's documentation to the database."
  [{:keys [username]} app-id {docs :documentation}]
  (when-let [current-docs (dp/get-documentation app-id)]
    (throw+ {:type   :clojure-commons.exception/exists
             :error  "App already has documentation"
             :app_id app-id}))
  (dp/add-documentation (v/get-valid-user-id username) docs app-id)
  (get-app-docs app-id))

(defn owner-add-app-docs
  "Adds an App's documentation to the database if the App is owned by the current user."
  [user app-id docs]
  (v/verify-app-ownership user (ap/get-app app-id))
  (add-app-docs user app-id docs))
