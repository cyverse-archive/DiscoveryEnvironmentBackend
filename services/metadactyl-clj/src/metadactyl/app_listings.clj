(ns metadactyl.app-listings
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.app-listing]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config]
        [metadactyl.util.conversions :only [to-long date->long]]
        [metadactyl.workspace])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]))

(defn- add-subgroups
  [group groups]
  (let [subgroups (filter #(= (:hid group) (:parent_hid %)) groups)
        subgroups (map #(add-subgroups % groups) subgroups)
        result    (if (empty? subgroups) group (assoc group :groups subgroups))
        result    (assoc result :template_count (:app_count group))
        result    (dissoc result :app_count :parent_hid :hid)]
    result))

(defn- format-my-public-apps-group
  "Formats the virtual group for the user's public apps."
  [workspace-id]
  {:id             :my-public-apps
   :name           "My public apps"
   :description    ""
   :workspace_id   workspace-id
   :is_public      false
   :template_count (count-public-apps-by-user (.getEmail current-user))})

(defn list-my-public-apps
  "Lists the public apps belonging to the user with the given workspace."
  [workspace params]
  (list-public-apps-by-user
   workspace
   (workspace-favorites-app-group-index)
   (.getEmail current-user)
   params))

(def ^:private virtual-group-fns
  {:my-public-apps {:format-group   format-my-public-apps-group
                    :format-listing list-my-public-apps}})

(defn- format-virtual-groups
  "Formats any virtual groups that should appear in a user's workspace."
  [workspace-id]
  (map (fn [[_ {f :format-group}]] (f workspace-id)) virtual-group-fns))

(defn- add-virtual-groups
  [group workspace-id]
  (if current-user
    (let [virtual-groups (format-virtual-groups workspace-id)
          actual-count   (count-apps-in-group-for-user
                           (:id group)
                           (.getEmail current-user))]
      (-> group
          (update-in [:groups] concat virtual-groups)
          (assoc :template_count actual-count)))))

(defn- format-app-group-hierarchy
  "Formats the app group hierarchy rooted at the app group with the given
   identifier."
  [user-workspace-id {root-id :root_analysis_group_id workspace-id :id}]
  (let [groups (get-app-group-hierarchy root-id)
        root   (first (filter #(= root-id (:hid %)) groups))
        result (add-subgroups root groups)]
    (if (= user-workspace-id workspace-id)
      (add-virtual-groups result workspace-id)
      result)))

(defn get-only-app-groups
  "Retrieves the list of app groups that are visible to a user."
  ([]
     (-> (.getUsername current-user)
         (get-or-create-workspace)
         (:id)
         (get-only-app-groups)))
  ([workspace-id]
     (let [workspaces   (get-visible-workspaces workspace-id)]
       (cheshire/encode
        {:groups (map (partial format-app-group-hierarchy workspace-id) workspaces)}))))

(defn get-public-app-groups
  "Retrieves the list of app groups that are visible to all users. TODO: refactor me."
  []
  (get-only-app-groups -1))

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
  (-> (assoc app :can_run (= (:step_count app) (:component_count app)))
      (dissoc :component_count)
      (format-app-timestamps)
      (format-app-ratings)
      (format-app-pipeline-eligibility)
      (assoc :can_favor true :can_rate true)))

(defn- list-apps-in-virtual-group
  "Formats a listing for a virtual group."
  [workspace group-id params]
  (let [group-key (keyword group-id)]
    (when-let [format-fns (group-key virtual-group-fns)]
      (assoc ((:format-group format-fns) (:id workspace))
        :templates (map format-app ((:format-listing format-fns) workspace params))))))

(defn- count-apps-in-group
  "Counts the number of apps in an app group, including virtual app groups that may be included."
  [{root-group-hid :root_analysis_group_id} {:keys [hid id] :as app-group}]
  (if (= root-group-hid hid)
    (count-apps-in-group-for-user id (.getEmail current-user))
    (count-apps-in-group-for-user id)))

(defn- get-apps-in-group
  "Gets the apps in an app group, including virtual app groups that may be included."
  [{root-group-hid :root_analysis_group_id :as workspace} {:keys [hid id]} params]
  (let [faves-index (workspace-favorites-app-group-index)]
    (if (= root-group-hid hid)
      (get-apps-in-group-for-user id workspace faves-index params (.getEmail current-user))
      (get-apps-in-group-for-user id workspace faves-index params))))

(defn- list-apps-in-real-group
  "This service lists all of the apps in a real app group and all of its descendents."
  [workspace app_group_id params]
  (let [app_group      (get-app-group app_group_id)
        root_group_hid (:root_analysis_group_id workspace)
        total          (count-apps-in-group workspace app_group)
        apps_in_group  (get-apps-in-group workspace app_group params)
        apps_in_group  (map format-app apps_in_group)]
    (assoc app_group
      :template_count total
      :templates apps_in_group)))

(defn list-apps-in-group
  "This service lists all of the apps in an app group and all of its
   descendents."
  [app-group-id params]
  (let [workspace (get-or-create-workspace (.getUsername current-user))]
    (cheshire/encode
     (or (list-apps-in-virtual-group workspace app-group-id params)
         (list-apps-in-real-group workspace app-group-id params)))))

(defn search-apps
  "This service searches for apps in the user's workspace and all public app
   groups, based on a search term."
  [params]
  (let [search_term (curl/url-decode (:search params))
        workspace (get-or-create-workspace (.getUsername current-user))
        total (count-search-apps-for-user search_term (:id workspace))
        search_results (search-apps-for-user
                        search_term
                        workspace
                        (workspace-favorites-app-group-index)
                        params)
        search_results (map format-app search_results)]
    (cheshire/encode {:template_count total
                      :templates search_results})))

(defn- load-app-details
  "Retrieves the details for a single app."
  [app-id]
  (first (select transformation_activity
                 (with transformation_activity_references)
                 (where {:id app-id}))))

(defn- load-deployed-components
  "Loads information about the deployed components associated with an app."
  [app-id]
  (select [:deployed_components :dc]
          (fields
            :dc.id
            :dc.name
            :dc.description
            :dc.location
            [:tt.name :type]
            :dc.version
            :dc.attribution)
          (join [:tool_types :tt]
                {:dc.tool_type_id :tt.id})
          (join [:template :t]
                {:dc.id :t.component_id})
          (join [:transformations :tx]
                {:t.id :tx.template_id})
          (join [:transformation_steps :ts]
                {:tx.id :ts.transformation_id})
          (join [:transformation_task_steps :tts]
                {:ts.id :tts.transformation_step_id})
          (join [:transformation_activity :a]
                {:tts.transformation_task_id :a.hid})
          (where {:a.id app-id})))

(defn- timestamp-to-millis
  "Converts a timestamp, which may be nil, to a string representing the number
   of milliseconds since January 1, 1970."
  [timestamp]
  (if (nil? timestamp)
    ""
    (str (.getTime timestamp))))

(defn- format-app-details
  "Formats information for the get-app-details service."
  [details components]
  (let [app-id (:id details)]
    {:published_date   (timestamp-to-millis (:integration_date details))
     :edited_date      (timestamp-to-millis (:edited_date details))
     :id               app-id
     :references       (map :reference_text (:transformation_activity_references details))
     :description      (:description details "")
     :name             (:name details "")
     :label            (:label details "")
     :tito             app-id
     :components       components
     :groups           (get-groups-for-app app-id)
     :suggested_groups (get-suggested-groups-for-app app-id)}))

(defn get-app-details
  "This service obtains the high-level details of an app."
  [app-id]
  (let [details    (load-app-details app-id)
        components (load-deployed-components app-id)]
    (when (nil? details)
      (throw (IllegalArgumentException. (str "app, " app-id ", not found"))))
    (when (empty? components)
      (throw  (IllegalArgumentException. (str "no tools associated with app, " app-id))))
    (cheshire/encode (format-app-details details components))))

(defn load-app-ids
  "Loads the identifiers for all apps that refer to valid deployed components from the database."
  []
  (map :id
       (select [:transformation_activity :app]
               (modifier "distinct")
               (fields :app.id)
               (join [:transformation_task_steps :tts]
                     {:app.hid :tts.transformation_task_id})
               (join [:transformation_steps :ts]
                     {:tts.transformation_step_id :ts.id})
               (join [:transformations :tx]
                     {:ts.transformation_id :tx.id})
               (where (not [(sqlfn :exists (subselect [:template :t]
                                                      (join [:deployed_components :dc]
                                                            {:t.component_id :dc.id})
                                                      (where {:tx.template_id :t.id
                                                              :t.component_id nil})))]))
               (order :id :ASC))))

(defn get-all-app-ids
  "This service obtains the identifiers of all apps that refer to valid deployed components."
  []
  (cheshire/encode {:analysis_ids (load-app-ids)}))

(defn get-app-description
  "This service obtains the description of an app."
  [app-id]
  (:description (first (select transformation_activity (where {:id app-id}))) ""))
