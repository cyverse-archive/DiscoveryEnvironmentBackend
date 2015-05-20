(ns metadactyl.service.app-documentation
  (:use [metadactyl.persistence.app-documentation :only [add-documentation
                                                         edit-documentation
                                                         get-app-references
                                                         get-documentation]]
        [metadactyl.persistence.app-metadata :only [get-app]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.service :only [success-response]]
        [metadactyl.validation :only [get-valid-user-id verify-app-ownership]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as err]))

(defn- get-references
  "Returns a list of references from the database for the given app ID."
  [app-id]
  (map :reference_text (get-app-references app-id)))

(defn get-app-docs
  "Retrieves documentation details for the given app ID."
  [app-id]
  (if-let [docs (get-documentation app-id)]
    (success-response (assoc docs :references (get-references app-id)))
    (throw+ {:error_code err/ERR_NOT_FOUND
             :reason "App documentation not found"
             :app_id app-id})))

(defn add-app-docs
  "Adds an App's documentation to the database."
  [app-id {docs :documentation}]
  (when-let [current-docs (get-documentation app-id)]
    (throw+ {:error_code err/ERR_EXISTS
             :reason "App already has documentation"
             :app_id app-id}))
  (add-documentation
    (get-valid-user-id (:username current-user))
    docs
    app-id)
  (get-app-docs app-id))
