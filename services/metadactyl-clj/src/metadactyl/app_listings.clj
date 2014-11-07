(ns metadactyl.app-listings
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.app-listing]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.persistence.app-metadata :only [get-app get-app-tools]]
        [metadactyl.tools :only [get-tools-by-id]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config]
        [metadactyl.util.conversions :only [to-long remove-nil-vals]]
        [metadactyl.workspace])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [metadactyl.util.service :as service]))

(def my-public-apps-id (uuidify "00000000-0000-0000-0000-000000000000"))
(def trash-category-id (uuidify "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"))

(defn- add-subgroups
  [group groups]
  (let [subgroups (filter #(= (:id group) (:parent_id %)) groups)
        subgroups (map #(add-subgroups % groups) subgroups)
        result    (if (empty? subgroups) group (assoc group :categories subgroups))
        result    (dissoc result :parent_id :workspace_id :description)]
    result))

(defn format-trash-category
  "Formats the virtual group for the admin's deleted and orphaned apps category."
  [workspace-id params]
  {:id         trash-category-id
   :name       "Trash"
   :is_public  true
   :app_count  (count-deleted-and-orphaned-apps)})

(defn list-trashed-apps
  "Lists the public, deleted apps and orphaned apps."
  [workspace params]
  (list-deleted-and-orphaned-apps params))

(defn- format-my-public-apps-group
  "Formats the virtual group for the user's public apps."
  [workspace-id params]
  {:id        my-public-apps-id
   :name      "My public apps"
   :is_public false
   :app_count (count-public-apps-by-user (:email current-user) params)})

(defn list-my-public-apps
  "Lists the public apps belonging to the user with the given workspace."
  [workspace params]
  (list-public-apps-by-user
   workspace
   (workspace-favorites-app-group-index)
   (:email current-user)
   params))

(def ^:private virtual-group-fns
  {(keyword (str my-public-apps-id)) {:format-group   format-my-public-apps-group
                                      :format-listing list-my-public-apps}
   (keyword (str trash-category-id)) {:format-group   format-trash-category
                                      :format-listing list-trashed-apps}})

(defn- format-private-virtual-groups
  "Formats any virtual groups that should appear in a user's workspace."
  [workspace-id params]
  (remove :is_public
    (map (fn [[_ {f :format-group}]] (f workspace-id params)) virtual-group-fns)))

(defn- add-private-virtual-groups
  [group workspace-id params]
  (if current-user
    (let [virtual-groups (format-private-virtual-groups workspace-id params)
          actual-count   (count-apps-in-group-for-user
                           (:id group)
                           (:email current-user)
                           params)]
      (-> group
          (update-in [:categories] concat virtual-groups)
          (assoc :app_count actual-count)))))

(defn- format-app-group-hierarchy
  "Formats the app group hierarchy rooted at the app group with the given
   identifier."
  [user-workspace-id params {root-id :root_category_id workspace-id :id}]
  (let [groups (get-app-group-hierarchy root-id params)
        root   (first (filter #(= root-id (:id %)) groups))
        result (add-subgroups root groups)]
    (if (= user-workspace-id workspace-id)
      (add-private-virtual-groups result workspace-id params)
      result)))

(defn get-workspace-app-groups
  "Retrieves the list of the current user's workspace app groups."
  [params]
  (let [workspace (get-workspace)
        workspace-id (:id workspace)]
    {:categories [(format-app-group-hierarchy workspace-id params workspace)]}))

(defn get-visible-app-groups
  "Retrieves the list of app groups that are visible to a user."
  ([params]
     (-> (get-workspace)
         (:id)
         (get-visible-app-groups params)))
  ([workspace-id params]
     (let [workspaces (get-visible-workspaces workspace-id)]
       {:categories (map (partial format-app-group-hierarchy workspace-id params) workspaces)})))

(defn get-app-groups
  "Retrieves the list of app groups that are visible to all users, the current user's app groups, or
   both, depending on the :public param."
  [{:keys [public] :as params}]
  (service/success-response
    (if (contains? params :public)
      (if-not public
        (get-workspace-app-groups params)
        (get-visible-app-groups nil params))
      (get-visible-app-groups params))))

(defn- validate-app-pipeline-eligibility
  "Validates an App for pipeline eligibility, throwing a slingshot stone ."
  [app]
  (let [app_id (:id app)
        step_count (:step_count app)
        overall_job_type (:overall_job_type app)]
    (if (< step_count 1)
      (throw+ {:reason
               (str "Analysis, "
                    app_id
                    ", has too few steps for a pipeline.")}))
    (if (> step_count 1)
      (throw+ {:reason
               (str "Analysis, "
                    app_id
                    ", has too many steps for a pipeline.")}))
    (if-not (= overall_job_type "executable")
      (throw+ {:reason
               (str "Job type, "
                    overall_job_type
                    ", can't currently be included in a pipeline.")}))))

(defn- format-app-pipeline-eligibility
  "Validates an App for pipeline eligibility, reformatting its :overall_job_type value, and
   replacing it with a :pipeline_eligibility map"
  [app]
  (let [pipeline_eligibility (try+
                              (validate-app-pipeline-eligibility app)
                              {:is_valid true
                               :reason ""}
                              (catch map? {:keys [reason]}
                                {:is_valid false
                                 :reason reason}))
        app (dissoc app :overall_job_type)]
    (assoc app :pipeline_eligibility pipeline_eligibility)))

(defn- format-app-ratings
  "Formats an App's :average_rating, :user_rating, and :comment_id values into a
   :rating map."
  [{:keys [average_rating total_ratings user_rating comment_id] :as app}]
  (-> app
    (dissoc :average_rating :total_ratings :user_rating :comment_id)
    (assoc :rating (remove-nil-vals
                     {:average average_rating
                      :total total_ratings
                      :user user_rating
                      :comment_id comment_id}))))

(defn format-app-listing
  "Formats certain app fields into types more suitable for the client."
  [app]
  (-> (assoc app :can_run (= (:task_count app) (:tool_count app)))
      (dissoc :tool_count :task_count)
      (format-app-ratings)
      (format-app-pipeline-eligibility)
      (assoc :can_favor true :can_rate true :app_type "DE")
      (remove-nil-vals)))

(defn- list-apps-in-virtual-group
  "Formats a listing for a virtual group."
  [workspace group-id params]
  (let [group-key (keyword (str group-id))]
    (when-let [format-fns (virtual-group-fns group-key)]
      (-> ((:format-group format-fns) (:id workspace) params)
          (assoc :apps (map format-app-listing ((:format-listing format-fns) workspace params)))))))

(defn- count-apps-in-group
  "Counts the number of apps in an app group, including virtual app groups that may be included."
  [{root-group-id :root_category_id} {:keys [id] :as app-group} params]
  (if (= root-group-id id)
    (count-apps-in-group-for-user id (:email current-user) params)
    (count-apps-in-group-for-user id params)))

(defn- get-apps-in-group
  "Gets the apps in an app group, including virtual app groups that may be included."
  [{root-group-id :root_category_id :as workspace} {:keys [id]} params]
  (let [faves-index (workspace-favorites-app-group-index)]
    (if (= root-group-id id)
      (get-apps-in-group-for-user id workspace faves-index params (:email current-user))
      (get-apps-in-group-for-user id workspace faves-index params))))

(defn- list-apps-in-real-group
  "This service lists all of the apps in a real app group and all of its descendents."
  [workspace category-id params]
  (let [app_group      (->> (get-app-category category-id)
                            (assert-not-nil [:category_id category-id])
                            remove-nil-vals)
        total          (count-apps-in-group workspace app_group params)
        apps_in_group  (get-apps-in-group workspace app_group params)
        apps_in_group  (map format-app-listing apps_in_group)]
    (assoc app_group
      :app_count total
      :apps apps_in_group)))

(defn list-apps-in-group
  "This service lists all of the apps in an app group and all of its
   descendents."
  [app-group-id params]
  (let [workspace (get-workspace)]
    (service/success-response
     (or (list-apps-in-virtual-group workspace app-group-id params)
         (list-apps-in-real-group workspace app-group-id params)))))

(defn search-apps
  "This service searches for apps in the user's workspace and all public app
   groups, based on a search term."
  [params]
  (let [search_term (curl/url-decode (:search params))
        workspace (get-workspace)
        total (count-search-apps-for-user search_term (:id workspace) params)
        search_results (search-apps-for-user
                        search_term
                        workspace
                        (workspace-favorites-app-group-index)
                        params)
        search_results (map format-app-listing search_results)]
    (service/success-response {:app_count total
                               :apps search_results})))

(defn- load-app-details
  "Retrieves the details for a single app."
  [app-id]
  (assert-not-nil [:app-id app-id]
    (first (select apps (with app_references) (where {:id app-id})))))

(defn- format-app-details
  "Formats information for the get-app-details service."
  [details tools]
  (let [app-id (:id details)]
    (-> details
      (select-keys [:id :integration_date :edited_date :deleted :disabled :wiki_url])
      (assoc :name                 (:name details "")
             :description          (:description details "")
             :references           (map :reference_text (:app_references details))
             :tools                (remove-nil-vals tools)
             :categories           (get-groups-for-app app-id)
             :suggested_categories (get-suggested-groups-for-app app-id)))))

(defn get-app-details
  "This service obtains the high-level details of an app."
  [app-id]
  (let [details (load-app-details app-id)
        tools   (get-app-tools app-id)]
    (when (empty? tools)
      (throw  (IllegalArgumentException. (str "no tools associated with app, " app-id))))
    (->> (format-app-details details tools)
         (remove-nil-vals)
         (service/success-response))))

(defn load-app-ids
  "Loads the identifiers for all apps that refer to valid tools from the database."
  []
  (map :id
       (select [:apps :app]
               (modifier "distinct")
               (fields :app.id)
               (join [:app_steps :step]
                     {:app.id :step.app_id})
               (where (not [(sqlfn :exists (subselect [:tasks :t]
                                                      (join [:tools :dc]
                                                            {:t.tool_id :dc.id})
                                                      (where {:t.id :step.task_id
                                                              :t.tool_id nil})))]))
               (order :id :ASC))))

(defn get-all-app-ids
  "This service obtains the identifiers of all apps that refer to valid tools."
  []
  (service/success-response {:app_ids (load-app-ids)}))

(defn get-app-description
  "This service obtains the description of an app."
  [app-id]
  (:description (first (select apps (where {:id app-id}))) ""))

(defn- with-task-params
  "Includes a list of related file parameters in the query's result set,
   with fields required by the client."
  [query task-param-entity]
  (with query task-param-entity
              (join data_formats {:data_format :data_formats.id})
              (join :parameter_values {:parameter_values.parameter_id :id})
              (fields :id
                      :name
                      :label
                      :description
                      :required
                      :parameter_values.value
                      [:data_formats.name :format])))

(defn- get-tasks
  "Fetches a list of tasks for the given IDs with their inputs and outputs."
  [task-ids]
  (select tasks
    (fields :id
            :name
            :description)
    (with-task-params inputs)
    (with-task-params outputs)
    (where (in :id task-ids))))

(defn- format-task-file-param
  [file-parameter]
  (dissoc file-parameter :value))

(defn- format-task-output
  [{value :value :as output}]
  (-> output
    (assoc :label value)
    format-task-file-param))

(defn- format-task
  [task]
  (-> task
    (update-in [:inputs] (partial map (comp remove-nil-vals format-task-file-param)))
    (update-in [:outputs] (partial map (comp remove-nil-vals format-task-output)))))

(defn get-tasks-with-file-params
  "Fetches a formatted list of tasks for the given IDs with their inputs and outputs."
  [task-ids]
  (map format-task (get-tasks task-ids)))

(defn- format-app-task-listing
  [{app-id :id :as app}]
  (let [task-ids (map :task_id (select :app_steps (fields :task_id) (where {:app_id app-id})))
        tasks    (get-tasks-with-file-params task-ids)]
    (-> app
        (select-keys [:id :name :description])
        (assoc :tasks tasks))))

(defn get-app-task-listing
  "A service used to list the file parameters in an app."
  [app-id]
  (let [app (get-app app-id)]
    (service/success-response (format-app-task-listing app))))

(defn get-app-tool-listing
  "A service to list the tools used by an app."
  [app-id]
  (let [app (get-app app-id)
        tasks (:tasks (first (select apps
                               (with tasks (fields :tool_id))
                               (where {:apps.id app-id}))))
        tool-ids (map :tool_id tasks)]
    (service/success-response {:tools (get-tools-by-id tool-ids)})))
