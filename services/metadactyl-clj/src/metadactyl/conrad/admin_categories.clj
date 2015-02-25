(ns metadactyl.conrad.admin-categories
  (:use [kameleon.app-groups :only [add-subgroup
                                    category-ancestor-of-subcategory?
                                    category-contains-apps?
                                    category-contains-subcategory?
                                    category-hierarchy-contains-apps?
                                    create-app-group
                                    decategorize-category
                                    delete-app-category
                                    get-app-category
                                    update-app-category]]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.app-listings :only [format-trash-category]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config :only [workspace-public-id]]
        [metadactyl.util.service :only [success-response]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]
            [metadactyl.service.apps :as apps]))

(def ^:private max-app-category-name-len 255)

(defn- validate-app-category-existence
  "Retrieves all app category fields from the database."
  [category-id]
  (assert-not-nil [:category_id category-id] (get-app-category category-id)))

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
  (when (category-contains-subcategory? parent-id name)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains a subcategory with that name"
             :parent_id  parent-id
             :name       name})))

(defn- validate-category-empty
  "Validates that the given App Category contains no Apps directly under it."
  [parent-id]
  (when (category-contains-apps? parent-id)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains Apps"
             :parent_id  parent-id})))

(defn- validate-category-not-ancestor-of-parent
  [category-id parent-id]
  (when (category-ancestor-of-subcategory? category-id parent-id)
    (throw+ {:error_code   ce/ERR_ILLEGAL_ARGUMENT
             :reason       "App Category is an ancestor of the destination Category"
             :category_id  category-id
             :parent_id    parent-id})))

(defn- validate-category-hierarchy-empty
  "Validates that the given App Category and its subcategories contain no Apps."
  [category-id requestor]
  (when (category-hierarchy-contains-apps? category-id)
    (throw+ {:error_code   ce/ERR_ILLEGAL_ARGUMENT
             :reason       "App Category, or one of its subcategories, still contain Apps"
             :category_id  category-id
             :requested_by requestor})))

(defn- delete-valid-app-category
  [category-id]
  (let [category (get-app-category category-id)
        has-apps? (category-hierarchy-contains-apps? category-id)]
    (if (and category (not has-apps?))
      (do
        (delete-app-category category-id)
        (log/warn (:username current-user) "deleting category"
                  (:name category) "(" category-id ")"
                  "and all of its subcategoires"))
      category-id)))

(defn delete-categories
  "Deletes App Categories and all of their subcategories. Returns a list of category IDs that could
  not (or no longer) be found in the database, including subcategories of a category already deleted
  earlier in the list."
  [body]
  (transaction
    (let [failed-ids (remove nil? (map delete-valid-app-category (:category_ids body)))]
      (success-response {:category_ids failed-ids}))))

(defn add-category
  "Adds an App Category to a parent Category, as long as that parent does not contain any Apps."
  [{:keys [name parent_id] :as category}]
  (validate-app-category-existence parent_id)
  (validate-app-category-name name)
  (validate-subcategory-name parent_id name)
  (validate-category-empty parent_id)
  (transaction
    (let [category-id (:id (create-app-group (uuidify (workspace-public-id)) category))]
      (add-subgroup parent_id category-id)
      (apps/list-apps-in-category nil category-id {}))))

(defn delete-category
  "Deletes an App Category and all of its children, as long as they do not contain any Apps."
  [category-id]
  (let [requesting-user (:username current-user)
        category (validate-app-category-existence category-id)]
    (validate-category-hierarchy-empty category-id requesting-user)
    (log/warn requesting-user "deleting category"
                              (:name category) "(" category-id ")"
                              "and all of its subcategoires")
    (delete-app-category category-id)
    (success-response)))

(defn update-category
  "Updates an App Category's name or parent Category."
  [{category-id :id :keys [name parent_id] :as category}]
  (transaction
    (let [category (validate-app-category-existence category-id)]
      (when name
        (validate-app-category-name name)
        (update-app-category category-id name))
      (when parent_id
        (validate-subcategory-name parent_id (or name (:name category)))
        (validate-category-empty parent_id)
        (decategorize-category category-id)
        (validate-category-not-ancestor-of-parent category-id parent_id)
        (add-subgroup parent_id category-id))
      (apps/list-apps-in-category nil category-id {}))))

(defn get-admin-app-categories
  "Lists public App Categories with the Trash Category"
  [params]
  (-> (apps/get-app-categories nil params)
      (update-in [:categories] concat [(format-trash-category nil params)])
      success-response))
