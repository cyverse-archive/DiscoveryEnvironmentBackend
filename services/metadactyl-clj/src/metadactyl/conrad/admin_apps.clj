(ns metadactyl.conrad.admin-apps
  (:use [metadactyl.util.service :only [success-response]])
  (:require [metadactyl.persistence.app-metadata :as persistence]))

(defn- validate-app-existence
  "Verifies that apps exist."
  [app-id]
  (persistence/get-app app-id))

(defn delete-app
  "This service marks an existing app as deleted in the database."
  [app-id]
  (validate-app-existence app-id)
  (persistence/delete-app app-id)
  (success-response))
