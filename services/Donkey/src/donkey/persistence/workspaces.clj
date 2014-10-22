(ns donkey.persistence.workspaces
  (:use [korma.core]
        [korma.db :only [with-db]]
        [kameleon.queries]
        [kameleon.app-groups]
        [donkey.util.config :only [get-default-app-categories workspace-root-app-category]])
  (:require [donkey.util.db :as db]))

(defn workspace-for-user
  "Loads the workspace for a user."
  [username]
  (first
   (with-db db/de
     (select [:workspace :w]
             (join [:users :u] {:w.user_id :u.id})
             (fields :w.id :w.is_public)
             (where {:u.username username})))))

(defn- create-default-workspace-subcategories
  "Adds the default app subcategories for the root category of the given workspace."
  [workspace]
  (dorun
    (map-indexed
      (fn [index name]
        (add-subgroup
          (:root_category_id workspace)
          index
          (:id (create-app-group (:id workspace) {:name name}))))
      (get-default-app-categories))))

(defn- create-workspace-with-default-app-categories
  "Creates a workspace for the given user, with a workspace-root-app-category and its default
   subcategories."
  [user_id]
  (let [new-workspace (create-workspace user_id)
        workspace_id (:id new-workspace)
        root_app_group (create-app-group
                         workspace_id
                         {:name (workspace-root-app-category)})
        root-app-group-id (:id root_app_group)
        new-workspace (set-workspace-root-app-group
                        workspace_id
                        root-app-group-id)]
    (create-default-workspace-subcategories new-workspace)
    (assoc new-workspace :newWorkspace true)))

(defn get-or-create-workspace
  "Gets or creates a workspace database entry for the given username (which is also fetched or
   created). If a workspace is created via create-workspace-with-default-app-categories, then a
   workspace root group will be created with its default subcategories."
  [username]
  (with-db db/de
    (let [user_id (get-user-id username)
          workspace (fetch-workspace-by-user-id user_id)]
      (if-not (empty? workspace)
        (assoc workspace :newWorkspace false)
        (create-workspace-with-default-app-categories user_id)))))
