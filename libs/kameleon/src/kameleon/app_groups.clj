(ns kameleon.app-groups
  (:use [kameleon.entities]
        [kameleon.queries :only [add-agave-pipeline-where-clause]]
        [korma.core]
        [slingshot.slingshot :only [throw+]])
  (:require [kameleon.uuids :refer [uuid]]))

(defn get-app-group-hierarchy
  "Gets the app group hierarchy rooted at the node with the given identifier."
  [root-id {:keys [agave-enabled] :or {agave-enabled "false"}}]
  (select (sqlfn :app_category_hierarchy root-id (Boolean/parseBoolean agave-enabled))))

(defn get-visible-workspaces
  "Gets the list of workspaces that are visible to the user with the given workspace
   identifier."
  [workspace-id]
  (mapcat (fn [condition] (select workspace (where condition)))
          [{:id workspace-id} {:is_public true}]))

(defn get-visible-root-app-group-ids
  "Gets the list of internal root app group identifiers that are visible to the
   user with the given workspace identifier."
  [workspace-id]
  (map :root_category_id (get-visible-workspaces workspace-id)))

(defn load-root-app-groups-for-all-users
  "Gets the list of all root app group ids."
  []
  (select workspace
          (fields [:workspace.root_category_id :app_group_id]
                  [:workspace.id :workspace_id]
                  :users.username)
          (join users)))

(defn get-app-group
  "Retrieves an App category by its ID."
  [app_group_id]
  (first (select app_category_listing
                 (fields :id :name :description :is_public)
                 (where {:id app_group_id}))))

(defn create-app-group
  "Creates a database entry for an app group, with an UUID and the given
   workspace_id and name, and returns a map of the group with its id."
  ([workspace-id m]
     (create-app-group workspace-id (:name m) m))
  ([workspace-id name {:keys [id description] :or {:id (uuid)}}]
     (insert app_categories (values {:id           id
                                     :workspace_id workspace-id
                                     :description  description
                                     :name         name}))))

(defn add-subgroup
  "Adds a subgroup to a parent group, which should be listed at the given index
   position of the parent's subgroups."
  [parent-group-id index subgroup-id]
  (insert :app_category_group
          (values {:parent_category_id parent-group-id
                   :child_category_id subgroup-id
                   :child_index index})))

(defn is-subgroup?
  "Determines if one group is a subgroup of another."
  [parent-group-id subgroup-id]
  (pos? (count (select :app_category_group
                       (where {:parent_category_id parent-group-id
                               :child_category_id  subgroup-id})))))

(defn set-root-app-group
  "Sets the root app group for a workspace."
  [workspace-id root-group-id]
  (update workspace
          (set-fields {:root_category_id root-group-id})
          (where      {:id workspace-id})))

(defn decategorize-app
  "Removes an app from all categories in the database."
  [app-id]
  (delete :app_category_app (where {:app_id app-id})))

(defn get-app-by-id
  "Searches for an existing app by id."
  [id]
  (first (select apps
                 (where {:id id}))))

(defn app-in-group?
  "Determines whether or not an app is in an app group."
  [group-hid app-hid]
  (not (empty? (first (select :app_category_app
                              (where {:app_category_id group-hid
                                      :app_id          app-hid}))))))

(defn add-app-to-group
  "Adds an app to an app group."
  [group-id app-id]
  (when-not (app-in-group? group-id app-id)
    (insert :app_category_app
            (values {:app_category_id group-id
                     :app_id          app-id}))))

(defn get-groups-for-app
  "Retrieves a listing of all groups the app with the given ID is listed under."
  [app-id]
  (select app_category_listing
          (fields :id
                  :name)
          (join :app_category_app
                (= :app_category_app.app_category_id
                   :app_category_listing.id))
          (where {:app_category_app.app_id app-id
                  :is_public true})))

(defn get-suggested-groups-for-app
  "Retrieves a listing of all groups the integrator recommneds for the app."
  [app-id]
  (select :suggested_groups
          (fields :app_categories.id
                  :app_categories.name)
          (join :app_categories
                (= :app_categories.id
                   :suggested_groups.app_category_id))
          (where {:suggested_groups.app_id app-id})))
