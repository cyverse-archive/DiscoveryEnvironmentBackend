(ns kameleon.queries
  (:use [kameleon.core]
        [kameleon.entities]
        [korma.core :exclude [update]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [korma.core :as sql])
  (:import [java.sql Timestamp]))

(defn- version-table-exists?
  []
  (first
   (select :information_schema.tables
           (fields :table_name)
           (where {:table_name "version"}))))

(defn current-db-version
  "Determines the current database version."
  []
  (when (version-table-exists?)
    (-> (select version
                (fields [:version])
                (order :version :DESC)
                (limit 1))
        ffirst
        val)))

(defn check-db-version
  "Verifies that the current database version is the same as the version that
   is compatible with this version of Kameleon."
  []
  (let [current    (current-db-version)
        compatible (compatible-db-version)]
    (when (not= current compatible)
      (throw+ {:type ::incorrect-db-version
               :current current
               :compatible compatible}))))

(defn add-query-offset
  "Returns select_query with an OFFSET clause added if offset_val is 0 or more;
   otherwise the original select_query is returned."
  [select_query offset_val]
  (if (and offset_val (>= offset_val 0))
    (offset select_query offset_val)
    select_query))

(defn add-query-limit
  "Returns select_query with a LIMIT clause added if limit_val is more than 0;
   otherwise the original select_query is returned."
  [select_query limit_val]
  (if (and limit_val (> limit_val 0))
    (limit select_query limit_val)
    select_query))

(defn add-query-sorting
  "Returns select_query with an ORDER BY clause added if sort-field is not nil;
   otherwise the original select_query is returned."
  [select_query sort-field sort-dir]
  (if (not (nil? sort-field))
    (let [sort-dir (if (= sort-dir :DESC)
                     sort-dir
                     :ASC)]
      (order select_query sort-field sort-dir))
    select_query))

(defn get-collaborators
  "Gets the list of collaborators for a given fully qualified username."
  [username]
  (map :username
       (select collaborators
               (fields :collaborator.username)
               (join users)
               (join collaborator)
               (where {:users.username username}))))

(defn get-existing-user-id
  "Gets an existing user identifier for a fully qualified username. Returns nil if the
   username isn't found in the database."
  [username]
  ((comp :id first)
   (select :users (where {:username username}))))

(defn- get-new-user-id
  "Gets a new user identifier for a fully qualified username."
  [username]
  (insert :users (values {:username username}))
  (get-existing-user-id username))

(defn get-user-id
  "Gets the internal user identifier for a fully qualified username.  A new
   entry will be added if the user doesn't already exist in the database."
  [username]
  (or (get-existing-user-id username)
      (get-new-user-id username)))

(defn get-user-ids
  "Gets the internal user identifiers for each username in a collection of
   fully qualified usernames.  Entries will be added for users that don't exist
   in the database."
  [usernames]
  (map get-user-id usernames))

(defn get-username
  "Gets the username for the user with the given identifier."
  [user-id]
  (when-not (nil? user-id)
    (:username (first (select users (where {:id user-id}))))))

(defn- add-collaboration
  "Adds a collaboration to the database if the collaboration doesn't exist
   already."
  [user-id collaborator-id]
  (let [results (select collaborators
                        (where {:user_id user-id
                                :collaborator_id collaborator-id}))]
    (when (empty? results)
      (insert collaborators
              (values {:user_id user-id
                       :collaborator_id collaborator-id})))))

(defn add-collaborators
  "Adds collaborators for a given fully qualified username."
  [username collaborators]
  (let [user-id (get-user-id username)
        collaborator-ids (get-user-ids collaborators)]
    (transaction
      (dorun (map (partial add-collaboration user-id) collaborator-ids)))))

(defn- user-id-subquery
  "Performs a subquery for a user ID."
  [username]
  (subselect users
             (fields :id)
             (where {:username username})))

(defn- remove-collaboration
  "Removes a collaboration from the database if it exists."
  [user collab]
  (delete collaborators
          (where
           {:user_id         [= (user-id-subquery user)]
            :collaborator_id [= (user-id-subquery collab)]})))

(defn remove-collaborators
  "Removes collaborators for a given fully qualified username."
  [username collaborators]
  (transaction
    (dorun (map (partial remove-collaboration username) collaborators))))

(defn fetch-workspace-by-user-id
  "Gets the workspace for the given user_id."
  [user_id]
  (first (select workspace (where {:user_id user_id}))))

(defn create-workspace
  "Creates a workspace database entry for the given user ID."
  [user_id]
  (insert workspace (values {:user_id user_id})))

(defn set-workspace-root-app-group
  "Sets the given root-app-group ID in the given workspace, and returns a map of
   the workspace with its new group ID."
  [workspace_id root_app_group_id]
  (sql/update workspace
    (set-fields {:root_category_id root_app_group_id})
    (where {:id workspace_id})))

(defn parameter-types-for-tool-type
  "Lists the valid parameter types for the tool type with the given identifier."
  ([tool-type-id]
     (parameter-types-for-tool-type (select* parameter_types) tool-type-id))
  ([base-query tool-type-id]
     (select base-query
             (join :tool_type_parameter_type
                   {:tool_type_parameter_type.parameter_type_id
                    :parameter_types.id})
             (where {:tool_type_parameter_type.tool_type_id tool-type-id}))))

(defn get-tool-type-by-name
  "Searches for the tool type with the given name."
  [tool-type-name]
  (first (select tool_types
                 (where {:name tool-type-name}))))

(defn get-tool-type-by-component-id
  "Searches for the tool type associated with the given deployed component."
  [component-id]
  (first (select tools
                 (fields :tool_types.id :tool_types.name :tool_types.label
                         :tool_types.description)
                 (join tool_types)
                 (where {:tools.id component-id}))))

(defn get-public-user-id
  "Gets the user ID for the public user."
  []
  (get-user-id "<public>"))

(defn get-tasks-for-app
  "Retrieves the list of tasks associated with an app."
  [app-id]
  (select [:apps :a]
          (fields :t.id :t.external_app_id :t.name :t.description :t.label :t.tool_id)
          (join [:app_steps :step]
                {:a.id :step.app_id})
          (join [:tasks :t]
                {:step.task_id :t.id})
          (where {:a.id app-id})))

(defn get-tool-request-details
  "Obtains detailed information about a tool request."
  [uuid]
  (first
   (select [:tool_requests :tr]
           (fields :tr.id
                   [:requestor.username :submitted_by]
                   :tr.phone
                   [:tr.tool_name :name]
                   :tr.description
                   :tr.source_url
                   [:tr.doc_url :documentation_url]
                   :tr.version
                   :tr.attribution
                   :tr.multithreaded
                   [:architecture.name :architecture]
                   :tr.test_data_path
                   [:tr.instructions :cmd_line]
                   :tr.additional_info
                   :tr.additional_data_file)
           (join [:users :requestor]
                 {:tr.requestor_id :requestor.id})
           (join [:tool_architectures :architecture]
                 {:tr.tool_architecture_id :architecture.id})
           (where {:tr.id uuid}))))

(defn get-tool-request-history
  "Obtains detailed information about the history of a tool request."
  [uuid]
  (select [:tool_request_statuses :trs]
          (fields [:trsc.name :status]
                  [:trs.date_assigned :status_date]
                  [:updater.username :updated_by]
                  :trs.comments)
          (join [:tool_requests :tr]
                {:trs.tool_request_id :tr.id})
          (join [:users :updater]
                {:trs.updater_id :updater.id})
          (join [:tool_request_status_codes :trsc]
                {:trs.tool_request_status_code_id :trsc.id})
          (where {:tr.id uuid})
          (order :trs.date_assigned :ASC)))

(defn- remove-nil-values
  "Removes entries with nil values from a map."
  [m]
  (into {} (remove (fn [[_ v]] (nil? v)) m)))

(defmacro ^:private where-if-defined
  "Adds a where clause to a query, filtering out all conditions for which the value is nil."
  [query clause]
  `(where ~query (remove-nil-values ~clause)))

(defn- list-tool-requests-subselect
  "Creates a subselect query that can be used to list tool requests."
  [user]
  (subselect [:tool_requests :tr]
             (fields [:tr.id :id]
                     [:tr.tool_name :name]
                     [:tr.version :version]
                     [:trsc.name :status]
                     [:trs.date_assigned :status_date]
                     [:updater.username :updated_by]
                     [:requestor.username :requested_by])
             (join [:users :requestor] {:tr.requestor_id :requestor.id})
             (join [:tool_request_statuses :trs] {:tr.id :trs.tool_request_id})
             (join [:tool_request_status_codes :trsc]
                   {:trs.tool_request_status_code_id :trsc.id})
             (join [:users :updater] {:trs.updater_id :updater.id})
             (where-if-defined {:requestor.username user})
             (order :trs.date_assigned :ASC)))

(defn list-tool-requests
  "Lists the tool requests that have been submitted by the user."
  [& {user       :username
      row-offset :offset
      row-limit  :limit
      sort-field :sort-field
      sort-order :sort-order
      statuses   :statuses}]
  (let [status-clause (if (nil? statuses) nil ['in statuses])]
    (select
     [(subselect [(list-tool-requests-subselect user) :req]
                 (fields :id :name :version :requested_by
                         [(sqlfn :first :status_date) :date_submitted]
                         [(sqlfn :last :status) :status]
                         [(sqlfn :last :status_date) :date_updated]
                         [(sqlfn :last :updated_by) :updated_by])
                 (group :id :name :version :requested_by)
                 (order (or sort-field :date_submitted) (or sort-order :ASC))
                 (limit row-limit)
                 (offset row-offset))
      :reqs]
     (where-if-defined {:status status-clause}))))

(defn- insert-login-record
  "Recrds when a user logs into the DE."
  [user-id ip-address user-agent]
  (insert :logins
          (values {:user_id    user-id
                   :ip_address ip-address
                   :user_agent user-agent})))

(defn record-login
  "Records when a user logs into the DE. Returns the recorded login time."
  [username ip-address user-agent]
  (-> (insert-login-record (get-user-id username) ip-address user-agent)
      (:login_time)
      (.getTime)))

(defn record-logout
  "Records when a user logs out of the DE."
  [username ip-address login-time]
  (sql/update :logins
    (set-fields {:logout_time (sqlfn :now)})
    (where {:user_id                                       (get-user-id username)
            :ip_address                                    ip-address})
    (where {(sqlfn :date_trunc "milliseconds" :login_time) (Timestamp. login-time)})))

(defn add-agave-pipeline-where-clause
  [query {agave-enabled? :agave-enabled :or {agave-enaled? "false"}}]
  (let [agave-enabled? (Boolean/parseBoolean agave-enabled?)]
    (if-not agave-enabled?
      (where query {:step_count :task_count})
      query)))
