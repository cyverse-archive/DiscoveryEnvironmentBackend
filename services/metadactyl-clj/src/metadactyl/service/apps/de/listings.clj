(ns metadactyl.service.apps.de.listings
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core :exclude [update]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.app-listing]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.persistence.app-documentation :only [get-documentation]]
        [metadactyl.persistence.app-metadata :only [get-app get-app-tools]]
        [metadactyl.tools :only [get-tools-by-id]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config]
        [metadactyl.util.conversions :only [to-long remove-nil-vals]]
        [metadactyl.workspace])
  (:require [cemerick.url :as curl]))

(def my-public-apps-id (uuidify "00000000-0000-0000-0000-000000000000"))
(def trash-category-id (uuidify "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"))

(def default-sort-params
  {:sort-field :lower_case_name
   :sort-dir   :ASC})

(defn- fix-sort-params
  [params]
  (let [params (merge default-sort-params params)]
    (if (= (keyword (:sort-field params)) :name)
      (assoc params :sort-field (:sort-field default-sort-params))
      params)))

(defn- add-subgroups
  [group groups]
  (let [subgroups (filter #(= (:id group) (:parent_id %)) groups)
        subgroups (map #(add-subgroups % groups) subgroups)
        result    (if (empty? subgroups) group (assoc group :categories subgroups))
        result    (dissoc result :parent_id :workspace_id :description)]
    result))

(defn format-trash-category
  "Formats the virtual group for the admin's deleted and orphaned apps category."
  [user workspace-id params]
  {:id         trash-category-id
   :name       "Trash"
   :is_public  true
   :app_count  (count-deleted-and-orphaned-apps)})

(defn list-trashed-apps
  "Lists the public, deleted apps and orphaned apps."
  [user workspace params]
  (list-deleted-and-orphaned-apps params))

(defn- format-my-public-apps-group
  "Formats the virtual group for the user's public apps."
  [user workspace-id params]
  {:id        my-public-apps-id
   :name      "My public apps"
   :is_public false
   :app_count (count-public-apps-by-user (:email user) params)})

(defn list-my-public-apps
  "Lists the public apps belonging to the user with the given workspace."
  [user workspace params]
  (list-public-apps-by-user
   workspace
   (workspace-favorites-app-category-index)
   (:email user)
   params))

(def ^:private virtual-group-fns
  {(keyword (str my-public-apps-id)) {:format-group   format-my-public-apps-group
                                      :format-listing list-my-public-apps}
   (keyword (str trash-category-id)) {:format-group   format-trash-category
                                      :format-listing list-trashed-apps}})

(defn- format-private-virtual-groups
  "Formats any virtual groups that should appear in a user's workspace."
  [user workspace-id params]
  (remove :is_public
    (map (fn [[_ {f :format-group}]] (f user workspace-id params)) virtual-group-fns)))

(defn- add-private-virtual-groups
  [user group workspace-id params]
  (let [virtual-groups (format-private-virtual-groups user workspace-id params)
        actual-count   (count-apps-in-group-for-user
                        (:id group)
                        (:email user)
                        params)]
    (-> group
        (update-in [:categories] concat virtual-groups)
        (assoc :app_count actual-count))))

(defn- format-app-group-hierarchy
  "Formats the app group hierarchy rooted at the app group with the given
   identifier."
  [user user-workspace-id params {root-id :root_category_id workspace-id :id}]
  (let [groups (get-app-group-hierarchy root-id params)
        root   (first (filter #(= root-id (:id %)) groups))
        result (add-subgroups root groups)]
    (if (= user-workspace-id workspace-id)
      (add-private-virtual-groups user result workspace-id params)
      result)))

(defn get-workspace-app-groups
  "Retrieves the list of the current user's workspace app groups."
  [user params]
  (let [workspace (get-workspace (:username user))
        workspace-id (:id workspace)]
    [(format-app-group-hierarchy user workspace-id params workspace)]))

(defn get-visible-app-groups-for-workspace
  "Retrieves the list of app groups that."
  [workspace-id user params]
  (let [workspaces (get-visible-workspaces workspace-id)]
    (map (partial format-app-group-hierarchy user workspace-id params) workspaces)))

(defn get-visible-app-groups
  "Retrieves the list of app groups that are visible to a user."
  [user params]
  (-> (get-optional-workspace (:username user))
      (:id)
      (get-visible-app-groups-for-workspace user params)))

(defn get-app-groups
  "Retrieves the list of app groups that are visible to all users, the current user's app groups, or
   both, depending on the :public param."
  [user {:keys [public] :as params}]
  (if (contains? params :public)
    (if-not public
      (get-workspace-app-groups user params)
      (get-visible-app-groups-for-workspace nil user params))
    (get-visible-app-groups user params)))

(defn get-admin-app-groups
  "Retrieves the list of app groups that are accessible to administrators. This includes all public
   app groups along with the trash group."
  [params]
  (conj (vec (get-app-groups nil params))
        (format-trash-category nil nil params)))

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

(defn- app-can-run?
  [{tool-count :tool_count external-app-count :external_app_count task-count :task_count}]
  (= (+ tool-count external-app-count) task-count))

(defn format-app-listing
  "Formats certain app fields into types more suitable for the client."
  [app]
  (-> (assoc app :can_run (app-can-run? app))
      (dissoc :tool_count :task_count :external_app_count :lower_case_name)
      (format-app-ratings)
      (format-app-pipeline-eligibility)
      (assoc :can_favor true :can_rate true :app_type "DE")
      (remove-nil-vals)))

(defn- list-apps-in-virtual-group
  "Formats a listing for a virtual group."
  [user workspace group-id params]
  (let [group-key (keyword (str group-id))]
    (when-let [format-fns (virtual-group-fns group-key)]
      (-> ((:format-group format-fns) user (:id workspace) params)
          (assoc :apps (->> ((:format-listing format-fns) user workspace params)
                            (map format-app-listing)))))))

(defn- count-apps-in-group
  "Counts the number of apps in an app group, including virtual app groups that may be included."
  [user {root-group-id :root_category_id} {:keys [id] :as app-group} params]
  (if (= root-group-id id)
    (count-apps-in-group-for-user id (:email user) params)
    (count-apps-in-group-for-user id params)))

(defn- get-apps-in-group
  "Gets the apps in an app group, including virtual app groups that may be included."
  [user {root-group-id :root_category_id :as workspace} {:keys [id]} params]
  (let [faves-index (workspace-favorites-app-category-index)]
    (if (= root-group-id id)
      (get-apps-in-group-for-user id workspace faves-index params (:email user))
      (get-apps-in-group-for-user id workspace faves-index params))))

(defn- list-apps-in-real-group
  "This service lists all of the apps in a real app group and all of its descendents."
  [user workspace category-id params]
  (let [app_group      (->> (get-app-category category-id)
                            (assert-not-nil [:category_id category-id])
                            remove-nil-vals)
        total          (count-apps-in-group user workspace app_group params)
        apps_in_group  (get-apps-in-group user workspace app_group params)
        apps_in_group  (map format-app-listing apps_in_group)]
    (assoc app_group
      :app_count total
      :apps apps_in_group)))

(defn list-apps-in-group
  "This service lists all of the apps in an app group and all of its
   descendents."
  [user app-group-id params]
  (let [workspace (get-optional-workspace (:username user))
        params    (fix-sort-params params)]
    (or (list-apps-in-virtual-group user workspace app-group-id params)
        (list-apps-in-real-group user workspace app-group-id params))))

(defn has-category
  "Determines whether or not a category with the given ID exists."
  [category-id]
  (or (#{my-public-apps-id trash-category-id} category-id)
      (seq (select :app_categories (where {:id category-id})))))

(defn search-apps
  "This service searches for apps in the user's workspace and all public app
   groups, based on a search term."
  [user params]
  (let [search_term (curl/url-decode (:search params))
        workspace (get-workspace (:username user))
        total (count-search-apps-for-user search_term (:id workspace) params)
        search_results (search-apps-for-user
                        search_term
                        workspace
                        (workspace-favorites-app-category-index)
                        (fix-sort-params params))
        search_results (map format-app-listing search_results)]
    {:app_count total
     :apps search_results}))

(defn- load-app-details
  "Retrieves the details for a single app."
  [app-id]
  (assert-not-nil [:app-id app-id]
    (first (select apps
                   (with app_references)
                   (with integration_data)
                   (where {:id app-id})))))

(defn- format-wiki-url
  "CORE-6510: Remove the wiki_url from app details responses if the App has documentation saved."
  [{:keys [id wiki_url] :as app}]
  (assoc app :wiki_url (if (get-documentation id) nil wiki_url)))

(defn- format-app-details
  "Formats information for the get-app-details service."
  [details tools]
  (let [app-id (:id details)]
    (-> details
      (select-keys [:id :integration_date :edited_date :deleted :disabled :wiki_url
                    :integrator_name :integrator_email])
      (assoc :name                 (:name details "")
             :description          (:description details "")
             :references           (map :reference_text (:app_references details))
             :tools                (map remove-nil-vals tools)
             :categories           (get-groups-for-app app-id)
             :suggested_categories (get-suggested-groups-for-app app-id))
      format-wiki-url)))

(defn get-app-details
  "This service obtains the high-level details of an app."
  [app-id]
  (let [details (load-app-details app-id)
        tools   (get-app-tools app-id)]
    (when (empty? tools)
      (throw  (IllegalArgumentException. (str "no tools associated with app, " app-id))))
    (->> (format-app-details details tools)
         (remove-nil-vals))))

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

(defn list-app-ids
  "This service obtains the identifiers of all apps that refer to valid tools."
  []
  {:app_ids (load-app-ids)})

(defn- with-task-params
  "Includes a list of related file parameters in the query's result set,
   with fields required by the client."
  [query task-param-entity]
  (with query task-param-entity
              (join :parameter_values {:parameter_values.parameter_id :id})
              (fields :id
                      :name
                      :label
                      :description
                      :required
                      :parameter_values.value
                      [:data_format :format])))

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
    (format-app-task-listing app)))

(defn get-app-tool-listing
  "A service to list the tools used by an app."
  [app-id]
  (let [app (get-app app-id)
        tasks (:tasks (first (select apps
                               (with tasks (fields :tool_id))
                               (where {:apps.id app-id}))))
        tool-ids (map :tool_id tasks)]
    {:tools (get-tools-by-id tool-ids)}))
