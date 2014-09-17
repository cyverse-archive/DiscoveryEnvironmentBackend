(ns metadactyl.app-listings
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.app-listing]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config]
        [metadactyl.util.conversions :only [to-long date->long remove-nil-vals]]
        [metadactyl.workspace])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [medley.core :as medley]
            [metadactyl.util.service :as service]))

(defn- add-subgroups
  [group groups]
  (let [subgroups (filter #(= (:id group) (:parent_id %)) groups)
        subgroups (map #(add-subgroups % groups) subgroups)
        result    (if (empty? subgroups) group (assoc group :groups subgroups))
        result    (dissoc result :parent_id)]
    result))

(defn- format-my-public-apps-group
  "Formats the virtual group for the user's public apps."
  [workspace-id params]
  {:id           (uuidify "00000000-0000-0000-0000-000000000000")
   :name         "My public apps"
   :description  ""
   :workspace_id workspace-id
   :is_public    false
   :app_count    (count-public-apps-by-user (:email current-user) params)})

(defn list-my-public-apps
  "Lists the public apps belonging to the user with the given workspace."
  [workspace params]
  (list-public-apps-by-user
   workspace
   (workspace-favorites-app-group-index)
   (:email current-user)
   params))

(def ^:private virtual-group-fns
  {:00000000-0000-0000-0000-000000000000 {:format-group   format-my-public-apps-group
                                          :format-listing list-my-public-apps}})

(defn- format-virtual-groups
  "Formats any virtual groups that should appear in a user's workspace."
  [workspace-id params]
  (map (fn [[_ {f :format-group}]] (f workspace-id params)) virtual-group-fns))

(defn- add-virtual-groups
  [group workspace-id params]
  (if current-user
    (let [virtual-groups (format-virtual-groups workspace-id params)
          actual-count   (count-apps-in-group-for-user
                           (:id group)
                           (:email current-user)
                           params)]
      (-> group
          (update-in [:groups] concat virtual-groups)
          (assoc :app_count actual-count)))))

(defn- format-app-group-hierarchy
  "Formats the app group hierarchy rooted at the app group with the given
   identifier."
  [user-workspace-id params {root-id :root_category_id workspace-id :id}]
  (let [groups (get-app-group-hierarchy root-id params)
        root   (first (filter #(= root-id (:id %)) groups))
        result (add-subgroups root groups)]
    (if (= user-workspace-id workspace-id)
      (add-virtual-groups result workspace-id params)
      result)))

(defn get-workspace-app-groups
  "Retrieves the list of the current user's workspace app groups."
  [params]
  (let [workspace (get-or-create-workspace (:username current-user))
        workspace-id (:id workspace)]
    {:groups [(format-app-group-hierarchy workspace-id params workspace)]}))

(defn get-visible-app-groups
  "Retrieves the list of app groups that are visible to a user."
  ([params]
     (-> (:username current-user)
         (get-or-create-workspace)
         (:id)
         (get-visible-app-groups params)))
  ([workspace-id params]
     (let [workspaces (get-visible-workspaces workspace-id)]
       {:groups (map (partial format-app-group-hierarchy workspace-id params) workspaces)})))

(defn get-app-groups
  "Retrieves the list of app groups that are visible to all users, the current user's app groups, or
   both, depending on the :public param."
  [{:keys [public] :as params}]
  (service/swagger-response
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
  [app]
  (let [average_rating (:average_rating app)
        user_rating (:user_rating app)
        comment_id (:comment_id app)
        rating (if (not (or (nil? user_rating) (nil? comment_id)))
                 {:average average_rating
                  :user user_rating
                  :comment_id comment_id}
                 {:average average_rating})
        app (dissoc app :average_rating :user_rating :comment_id)]
    (assoc app :rating rating)))

(defn- format-app-timestamps
  "Formats each timestamp in an app."
  [app]
  (let [edited_date (date->long (:edited_date app))
        integration_date (date->long (:integration_date app))]
    (assoc app :edited_date edited_date :integration_date integration_date)))

(defn- format-app
  "Formats certain app fields into types more suitable for the client."
  [app]
  (-> (assoc app :can_run (= (:task_count app) (:tool_count app)))
      (dissoc :tool_count :task_count)
      (format-app-timestamps)
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
          (dissoc :workspace_id)
          (assoc :apps (map format-app ((:format-listing format-fns) workspace params)))))))

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
  [workspace app_group_id params]
  (let [app_group      (medley/remove-vals nil? (get-app-group app_group_id))
        total          (count-apps-in-group workspace app_group params)
        apps_in_group  (get-apps-in-group workspace app_group params)
        apps_in_group  (map format-app apps_in_group)]
    (assoc app_group
      :app_count total
      :apps apps_in_group)))

(defn list-apps-in-group
  "This service lists all of the apps in an app group and all of its
   descendents."
  [app-group-id params]
  (let [workspace (get-or-create-workspace (:username current-user))]
    (service/swagger-response
     (or (list-apps-in-virtual-group workspace app-group-id params)
         (list-apps-in-real-group workspace app-group-id params)))))

(defn search-apps
  "This service searches for apps in the user's workspace and all public app
   groups, based on a search term."
  [params]
  (let [search_term (curl/url-decode (:search params))
        workspace (get-or-create-workspace (:username current-user))
        total (count-search-apps-for-user search_term (:id workspace) params)
        search_results (search-apps-for-user
                        search_term
                        workspace
                        (workspace-favorites-app-group-index)
                        params)
        search_results (map format-app search_results)]
    (service/swagger-response {:app_count total
                               :apps search_results})))

(defn- load-app-details
  "Retrieves the details for a single app."
  [app-id]
  (first (select apps
                 (with app_references)
                 (where {:id app-id}))))

(defn- load-tools
  "Loads information about the deployed components associated with an app."
  [app-id]
  (select tool_listing
          (fields
            [:tool_id :id]
            :name
            :description
            :location
            :type
            :version
            :attribution)
          (where {:app_id app-id})))

(defn- timestamp-to-millis
  "Converts a timestamp, which may be nil, to the number of milliseconds since January 1, 1970."
  [timestamp]
  (if (nil? timestamp)
    nil
    (.getTime timestamp)))

(defn- format-app-details
  "Formats information for the get-app-details service."
  [details tools]
  (let [app-id (:id details)]
    {:id                   app-id
     :name                 (:name details "")
     :description          (:description details "")
     :integration_date     (timestamp-to-millis (:integration_date details))
     :edited_date          (timestamp-to-millis (:edited_date details))
     :references           (map :reference_text (:app_references details))
     :tools                tools
     :categories           (get-groups-for-app app-id)
     :suggested_categories (get-suggested-groups-for-app app-id)}))

(defn get-app-details
  "This service obtains the high-level details of an app."
  [app-id]
  (let [details (load-app-details app-id)
        tools   (load-tools app-id)]
    (when (nil? details)
      (throw (IllegalArgumentException. (str "app, " app-id ", not found"))))
    (when (empty? tools)
      (throw  (IllegalArgumentException. (str "no tools associated with app, " app-id))))
    (->> (format-app-details details tools)
         (remove-nil-vals)
         (service/swagger-response))))

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
  (service/swagger-response {:app_ids (load-app-ids)}))

(defn get-app-description
  "This service obtains the description of an app."
  [app-id]
  (:description (first (select apps (where {:id app-id}))) ""))
