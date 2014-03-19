(ns metadactyl.workspace
  (:use [korma.core]
        [kameleon.core]
        [kameleon.queries]
        [kameleon.app-groups]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config])
  (:require [cheshire.core :as cheshire]))

(defn- create-default-workspace-subgroups
  "Creates the workspace-default-app-groups database entries for the given
   workspace"
  [workspace]
  (dorun
    (map-indexed
      (fn [index name]
        (add-subgroup
          (:root_analysis_group_id workspace)
          index
          (:hid (create-app-group (:id workspace) name))))
      (cheshire/decode (workspace-default-app-groups) true))))

(defn- create-workspace-with-default-app-groups
  "Creates a workspace for the given user, with a workspace-root-app-group and
   its default subgroups listed in workspace-default-app-groups"
  [user_id]
  (let [new-workspace (create-workspace user_id)
        workspace_id (:id new-workspace)
        root_app_group (create-app-group
                         workspace_id
                         (workspace-root-app-group))
        root_analysis_group_id (:hid root_app_group)
        new-workspace (set-workspace-root-app-group
                        workspace_id
                        root_analysis_group_id)]
    (create-default-workspace-subgroups new-workspace)
    new-workspace))

(defn get-or-create-workspace
  "Gets or creates a workspace database entry for the given username (which is
   also fetched or created). If a workspace is created via
   create-workspace-with-default-app-groups, then a workspace root group will be
   created with its default subgroups listed in workspace-default-app-groups."
  [username]
  (let [user_id (get-user-id username)
        workspace (fetch-workspace-by-user-id user_id)]
    (if (empty? workspace)
      (create-workspace-with-default-app-groups user_id)
      workspace)))
