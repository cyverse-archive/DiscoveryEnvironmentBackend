(ns metadactyl.service.apps.de.docs
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-documentation :as dp]))

(defn- get-references
  "Returns a list of references from the database for the given app ID."
  [app-id]
  (map :reference_text (dp/get-app-references app-id)))

(defn get-app-docs
  "Retrieves documentation details for the given app ID."
  [app-id]
  (if-let [docs (dp/get-documentation app-id)]
    (assoc docs :references (get-references app-id))
    (throw+ {:error_code ce/ERR_NOT_FOUND
             :reason "App documentation not found"
             :app_id app-id})))
