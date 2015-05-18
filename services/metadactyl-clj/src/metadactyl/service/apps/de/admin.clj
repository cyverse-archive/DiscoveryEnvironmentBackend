(ns metadactyl.service.apps.de.admin
  (:use [kameleon.uuids :only [uuidify]]
        [korma.db :only [transaction]]
        [metadactyl.persistence.app-metadata.relabel :only [update-app-labels]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config :only [workspace-public-id]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [kameleon.app-groups :as app-groups]
            [metadactyl.persistence.app-metadata :as persistence]))

(def ^:private max-app-category-name-len 255)

(defn- validate-app-existence
  "Verifies that apps exist."
  [app-id]
  (persistence/get-app app-id))

(defn- validate-app-category-existence
  "Retrieves all app category fields from the database."
  [category-id]
  (assert-not-nil [:category_id category-id] (app-groups/get-app-category category-id)))

(defn- validate-app-category-name
  "Validates the length of an App Category name."
  [name]
  (when (> (count name) max-app-category-name-len)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "App Category name too long."
             :name       name})))

(defn- validate-subcategory-name
  "Validates that the given subcategory name is available under the given App Category parent ID."
  [parent-id name]
  (when (app-groups/category-contains-subcategory? parent-id name)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains a subcategory with that name"
             :parent_id  parent-id
             :name       name})))

(defn- validate-category-empty
  "Validates that the given App Category contains no Apps directly under it."
  [parent-id]
  (when (app-groups/category-contains-apps? parent-id)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains Apps"
             :parent_id  parent-id})))

(defn delete-app
  "This service marks an existing app as deleted in the database."
  [app-id]
  (validate-app-existence app-id)
  (persistence/delete-app true app-id)
  nil)

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
  [{app-id :id :keys [references groups] :as app}]
  (persistence/update-app app)
  (when-not (empty? references)
    (persistence/set-app-references app-id references))
  (when-not (empty? groups)
    (update-app-labels (select-keys app [:id :groups]))))

(defn update-app
  "This service updates high-level details and labels in an App, and can mark or unmark the app as
   deleted or disabled in the database."
  [{app-id :id :as app}]
  (validate-app-existence app-id)
  (transaction
    (if (empty? (select-keys app [:name :description :wiki_url :references :groups]))
      (update-app-deleted-disabled app)
      (update-app-details app))))

(defn add-category
  "Adds an App Category to a parent Category, as long as that parent does not contain any Apps."
  [{:keys [name parent_id] :as category}]
  (validate-app-category-existence parent_id)
  (validate-app-category-name name)
  (validate-subcategory-name parent_id name)
  (validate-category-empty parent_id)
  (transaction
   (let [category-id (:id (app-groups/create-app-group (uuidify (workspace-public-id)) category))]
     (app-groups/add-subgroup parent_id category-id)
     category-id)))
