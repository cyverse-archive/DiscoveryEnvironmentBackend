(ns kameleon.app-groups
  (:use [kameleon.entities]
        [korma.core]
        [slingshot.slingshot :only [throw+]])
  (:import [java.util UUID]))

(defn- uuid
  "Generates a random UUID."
  []
  (-> (UUID/randomUUID) (.toString)))

(defn get-app-group-hierarchy
  "Gets the app group hierarchy rooted at the node with the given identifier."
  [root-id]
  (select (sqlfn :app_category_hierarchy root-id)))

(defn- get-root-app-group-ids
  "Gets the internal identifiers for all app groups associated with workspaces
   that satisfy the given condition."
  [condition]
  (map :app_group_id
       (select workspace
               (fields [:root_analysis_group_id :app_group_id])
               (where condition))))

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
  (map :root_analysis_group_id (get-visible-workspaces workspace-id)))

(defn load-root-app-groups-for-all-users
  "Gets the list of all root app group ids."
  []
  (select workspace
          (fields [:workspace.root_analysis_group_id :app_group_id]
                  [:workspace.id :workspace_id]
                  :users.username)
          (join users)))

(defn get-app-group
  "Retrieves an App Group by its ID."
  [app_group_id]
  (first (select analysis_group_listing
                 (fields :id :hid :name :description :is_public)
                 (where {:id app_group_id}))))

(defn create-app-group
  "Creates a database entry for a template_group, with an UUID and the given
   workspace_id and name, and returns a map of the group with its new hid."
  ([workspace-id m]
     (create-app-group workspace-id (:name m) m))
  ([workspace-id name {:keys [id description] :or {:id (uuid)}}]
     (insert template_group (values {:id           id
                                     :workspace_id workspace-id
                                     :description  description
                                     :name         name}))))

(defn add-subgroup
  "Adds a subgroup to a parent group, which should be listed at the given index
   position of the parent's subgroups."
  [parent_group_id index subgroup_id]
  (insert :template_group_group
          (values {:parent_group_id parent_group_id
                   :subgroup_id subgroup_id
                   :hid index})))

(defn is-subgroup?
  "Determines if one group is a subgroup of another."
  [parent-group-id subgroup-id]
  (pos? (count (select :template_group_group
                       (where {:parent_group_id parent-group-id
                               :subgroup_id     subgroup-id})))))

(defn set-root-app-group
  "Sets the root app group for a workspace."
  [workspace-id root-group-id]
  (update workspace
          (set-fields {:root_analysis_group_id root-group-id})
          (where      {:id workspace-id})))

(defn decategorize-app
  "Removes an app from all categories in the database."
  [app-id]
  (delete :template_group_template
          (where {:template_id (subselect transformation_activity
                                          (fields :hid)
                                          (where {:id app-id}))})))

(defn get-app-by-id
  "Searches for an existing app by id."
  [id]
  (first (select transformation_activity
                 (where {:id id}))))

(defn app-in-group?
  "Determines whether or not an app is in an app group."
  [group-hid app-hid]
  (not (empty? (first (select :template_group_template
                              (where {:template_group_id group-hid
                                      :template_id       app-hid}))))))

(defn add-app-to-group
  "Adds an app to an app group."
  [group-hid app-hid]
  (when-not (app-in-group? group-hid app-hid)
    (insert :template_group_template
            (values {:template_group_id group-hid
                     :template_id       app-hid}))))

(defn get-groups-for-app
  "Retrieves a listing of all groups the app with the given ID is listed under."
  [app-id]
  (select analysis_group_listing
          (fields :id
                  :name)
          (join :workspace
                (= :analysis_group_listing.workspace_id
                   :workspace.id))
          (join :template_group_template
                (= :template_group_template.template_group_id
                   :analysis_group_listing.hid))
          (join analysis_listing
                (= :analysis_listing.hid
                   :template_group_template.template_id))
          (where {:analysis_listing.id app-id
                  :is_public true})))

(defn get-suggested-groups-for-app
  "Retrieves a listing of all groups the integrator recommneds for the app."
  [app-id]
  (select :suggested_groups
          (fields :template_group.id
                  :template_group.name)
          (join :template_group
                (= :template_group.hid
                   :suggested_groups.template_group_id))
          (join analysis_listing
                (= :analysis_listing.hid
                   :suggested_groups.transformation_activity_id))
          (where {:analysis_listing.id app-id})))
