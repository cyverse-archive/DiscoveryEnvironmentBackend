(ns metadactyl.conrad.admin-apps
  (:use [metadactyl.app-listings :only [format-app-listing]]
        [metadactyl.persistence.app-metadata.relabel :only [update-app-labels]]
        [metadactyl.util.service :only [success-response]]
        [korma.db :only [transaction]])
  (:require [metadactyl.persistence.app-metadata :as persistence]))

(defn- validate-app-existence
  "Verifies that apps exist."
  [app-id]
  (persistence/get-app app-id))

(defn delete-app
  "This service marks an existing app as deleted in the database."
  [app-id]
  (validate-app-existence app-id)
  (persistence/delete-app true app-id)
  (success-response))

(defn- update-app-deleted-disabled
  "Updates only an App's deleted or disabled flags in the database."
  [{app-id :id :keys [deleted disabled]}]
  (when-not (nil? deleted)
    (persistence/delete-app deleted app-id))
  (when-not (nil? disabled)
    (persistence/disable-app disabled app-id)))

(defn- update-app-details
  "Updates high-level details and labels in an App, including deleted and disabled flags in the
   database."
  [{app-id :id :keys [wiki_url references groups] :as app}]
  (persistence/update-app (assoc app :wikiurl wiki_url))
  (when-not (empty? references)
    (persistence/set-app-references app-id references))
  (when groups
    (update-app-labels (select-keys app [:id :groups]))))

(defn update-app
  "This service updates high-level details and labels in an App, and can mark or unmark the app as
   deleted or disabled in the database."
  [{app-id :id :as app}]
  (validate-app-existence app-id)
  (transaction
    (if (empty? (select-keys app [:name :description :wiki_url :references :groups]))
      (update-app-deleted-disabled app)
      (update-app-details app))
    (let [app-listing (persistence/get-app app-id)]
      (-> app-listing
          (assoc :wiki_url (:wikiurl app-listing))
          (dissoc :wikiurl)
          format-app-listing
          success-response))))
