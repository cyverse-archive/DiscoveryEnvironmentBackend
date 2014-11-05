(ns metadactyl.conrad.admin-categories
  (:use [kameleon.app-groups :only [add-subgroup
                                    category-contains-apps?
                                    category-contains-subcategory?
                                    category-hierarchy-contains-apps?
                                    create-app-group
                                    decategorize-app
                                    delete-app-category
                                    get-app-category]]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.app-listings :only [list-apps-in-group]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config :only [workspace-public-id]]
        [metadactyl.util.service :only [success-response]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(defn- validate-app-category-existence
  "Retrieves all app category fields from the database."
  [category-id]
  (assert-not-nil [:category_id category-id] (get-app-category category-id)))

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
  (when (category-contains-subcategory? parent_id name)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains a subcategory with that name"
             :parent_id  parent_id
             :name       name}))
  (when (category-contains-apps? parent_id)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     "Parent App Category already contains Apps"
             :parent_id  parent_id}))
  (transaction
    (let [category-id (:id (create-app-group (uuidify (workspace-public-id)) category))]
      (add-subgroup parent_id category-id)
      (list-apps-in-group category-id {}))))

(defn delete-category
  "Deletes an App Category and all of its children, as long as they do not contain any Apps."
  [category-id]
  (let [requesting-user (:username current-user)
        category (validate-app-category-existence category-id)]
    (when (category-hierarchy-contains-apps? category-id)
      (throw+ {:error_code   ce/ERR_ILLEGAL_ARGUMENT
               :reason       "App Category, or one of its subcategories, still contain Apps"
               :category_id  category-id
               :requested_by requesting-user}))
    (log/warn requesting-user "deleting category"
                              (:name category) "(" category-id ")"
                              "and all of its subcategoires")
    (delete-app-category category-id)
    (success-response)))
