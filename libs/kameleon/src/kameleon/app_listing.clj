(ns kameleon.app-listing
  (:use [korma.core]
        [kameleon.entities]
        [kameleon.queries]
        [kameleon.app-groups :only [get-visible-root-app-group-ids]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn- get-group-hid-subselect
  "Gets a subselect that fetches the hid for the given template_group ID."
  [app_group_id]
  (subselect :template_group
             (fields :hid)
             (where {:id app_group_id})))

(defn- get-all-group-ids-subselect
  "Gets a subselect that fetches the template_group and its subgroup IDs with
   the stored procedure app_category_hierarchy_ids."
  [app_group_id]
  (subselect
    (sqlfn :app_category_hierarchy_ids
           (get-group-hid-subselect app_group_id))))

(defn- get-fav-group-id-subselect
  "Gets a subselect that fetches the ID for the Favorites group at the given
   index under the template_group with the given ID."
  [workspace_root_group_id favorites_group_index]
  (subselect
    :template_group_group
    (fields :subgroup_id)
    (where {:parent_group_id workspace_root_group_id
            :hid favorites_group_index})))

(defn- get-is-fav-sqlfn
  "Gets a sqlfn that retuns true if the App ID in its subselect is found in the
   Favorites group with the ID returned by get-fav-group-id-subselect."
  [workspace_root_group_id favorites_group_index]
  (let [fav_group_id_subselect (get-fav-group-id-subselect
                                 workspace_root_group_id
                                 favorites_group_index)]
    (sqlfn* :exists
            (subselect
              :template_group_template
              (where {:template_group_template.template_id
                      :analysis_listing.hid})
              (where {:template_group_template.template_group_id
                      fav_group_id_subselect})))))

(defn- add-app-group-where-clause
  "Adds a where clause to an analysis listing query to restrict app results to
   an app group and all of its descendents."
  [base_listing_query app_group_id]
  (-> base_listing_query
    (join :template_group_template
          (= :template_group_template.template_id
             :analysis_listing.hid))
    (where {:template_group_template.template_group_id
            [in (get-all-group-ids-subselect app_group_id)]})))

(defn- add-app-group-plus-public-apps-where-clause
  "Adds a where clause to an analysis listing query to restrict app results to
   an app group and all of its descendents plus the set of public apps
   integrated by a user."
  [query app-group-id email]
  (-> query
    (join :template_group_template
          (= :template_group_template.template_id
             :analysis_listing.hid))
    (where (or {:template_group_template.template_group_id
                [in (get-all-group-ids-subselect app-group-id)]}
               {:integrator_email email
                :is_public        true}))))

(defn- add-public-apps-by-user-where-clause
  "Adds a where clause to an analysis listing query to restrict app results to
   the set of public apps integrated by a user."
  [query email]
  (where query
         {:integrator_email email
          :is_public        true}))

(defn- get-app-count-base-query
  "Returns a base query for counting the total number of apps in the
   analysis_listing table."
  []
  (->
    (select* analysis_listing)
    (fields (raw "count(DISTINCT analysis_listing.id) AS total"))
    (where {:deleted false})))

(defn count-apps-in-group-for-user
  "Counts all of the apps in an app group and all of its descendents."
  ([app-group-id]
    ((comp :total first)
      (-> (get-app-count-base-query)
        (add-app-group-where-clause app-group-id)
        (select))))
  ([app-group-id email]
    ((comp :total first)
      (-> (get-app-count-base-query)
        (add-app-group-plus-public-apps-where-clause app-group-id email)
        (select)))))

(defn- get-app-listing-base-query
  "Gets an analysis_listing select query, setting any query limits and sorting
   found in the query_opts, using the given workspace (as returned by
   fetch-workspace-by-user-id) to mark whether each app is a favorite and to
   include the user's rating in each app.."
  [workspace favorites_group_index query_opts]
  (let [user_id (:user_id workspace)
        workspace_root_group_id (:root_analysis_group_id workspace)
        row_offset (try (Integer/parseInt (:offset query_opts)) (catch Exception e 0))
        row_limit (try (Integer/parseInt (:limit query_opts)) (catch Exception e -1))
        sort_field (keyword (or (:sortField query_opts) (:sortfield query_opts)))
        sort_dir (keyword (or (:sortDir query_opts) (:sortdir query_opts)))

        ;; Bind the final query
        listing_query (->
                        (select* analysis_listing)
                        (modifier "DISTINCT")
                        (fields :id
                                :name
                                :description
                                :integrator_name
                                :integrator_email
                                :integration_date
                                :edited_date
                                [:wikiurl :wiki_url]
                                :average_rating
                                :is_public
                                :step_count
                                :component_count
                                :deleted
                                :disabled
                                :overall_job_type)
                        (where {:deleted false}))

        ;; Bind is_favorite subqueries
        is_fav_subselect (get-is-fav-sqlfn
                           workspace_root_group_id
                           favorites_group_index)

        ;; Add user's is_favorite column
        listing_query (-> listing_query
                        (fields [is_fav_subselect :is_favorite]))

        ;; Join the user's ratings
        listing_query (-> listing_query
                        (fields [:ratings.rating :user_rating]
                                :ratings.comment_id)
                        (join ratings
                              (and (= :ratings.transformation_activity_id
                                      :analysis_listing.hid)
                                   (= :ratings.user_id
                                      user_id))))]

    ;; Add limits and sorting, if required, and return the query
    (->
      listing_query
      (add-query-limit row_limit)
      (add-query-offset row_offset)
      (add-query-sorting sort_field sort_dir))))

(defn get-apps-in-group-for-user
  "Lists all of the apps in an app group and all of its descendents, using the
   given workspace (as returned by fetch-workspace-by-user-id) to mark
   whether each app is a favorite and to include the user's rating in each app."
  ([app-group-id workspace faves-index query-opts]
    (-> (get-app-listing-base-query workspace faves-index query-opts)
      (add-app-group-where-clause app-group-id)
      (select)))
  ([app-group-id workspace faves-index query-opts email]
    (-> (get-app-listing-base-query workspace faves-index query-opts)
      (add-app-group-plus-public-apps-where-clause app-group-id email)
      (select))))

(defn- get-public-group-ids-subselect
  "Gets a subselect that fetches the workspace template_group ID, public root
   group IDs, and their subgroup IDs with the stored procedure
   app_category_hierarchy_ids."
  [workspace_id]
  (let [root_app_ids (get-visible-root-app-group-ids workspace_id)
        select-ids-fn #(str "SELECT * FROM app_category_hierarchy_ids(" % ")")
        union_select_ids (str/join
                           " UNION "
                           (map select-ids-fn root_app_ids))]
    (raw (str "(" union_select_ids ")"))))

(defn- get-deployed-component-search-subselect
  "Gets a subselect that fetches deployed components with names matching the
   given search_term, inside an exists check, for each app in the main select."
  [search_term]
  (sqlfn* :exists
          (subselect
            :deployed_component_listing
            (where {:analysis_listing.hid
                    :deployed_component_listing.analysis_id})
            (where {(sqlfn lower :deployed_component_listing.name)
                    [like (sqlfn lower search_term)]}))))

(defn- add-search-where-clauses
  "Adds where clauses to a base App search query to restrict results to Apps
   that contain search_term in their name or description, in all public groups
   and groups under the given workspace_id."
  [base_search_query search_term workspace_id]
  (let [search_term (str/replace
                      search_term
                      #"[%_*?]"
                      {"%" "\\%",
                       "_" "\\_",
                       "*" "%",
                       "?" "_"})
        search_term (str "%" search_term "%")
        sql-lower #(sqlfn lower %)]
    (->
      base_search_query
      (join :template_group_template
            (= :template_group_template.template_id
               :analysis_listing.hid))
      (where {:template_group_template.template_group_id
              [in (get-public-group-ids-subselect workspace_id)]})
      (where
        (or
          {(sql-lower :name) [like (sql-lower search_term)]}
          {(sql-lower :description) [like (sql-lower search_term)]}
          {(sql-lower :integrator_name) [like (sql-lower search_term)]}
          (get-deployed-component-search-subselect search_term))))))

(defn count-search-apps-for-user
  "Counts App search results that contain search_term in their name or
   description, in all public groups and groups under the given workspace_id."
  [search_term workspace_id]
  (let [count_query (add-search-where-clauses
                      (get-app-count-base-query)
                      search_term
                      workspace_id)]
    (:total (first (select count_query)))))

(defn search-apps-for-user
  "Searches Apps that contain search_term in their name or description, in all
   public groups and groups in workspace (as returned by
   fetch-workspace-by-user-id), marking whether each app is a favorite and
   including the user's rating in each app by the user_id found in workspace."
  [search_term workspace favorites_group_index query_opts]
  (let [search_query (get-app-listing-base-query
                       workspace
                       favorites_group_index
                       query_opts)
        search_query (add-search-where-clauses
                        search_query
                        search_term
                        (:id workspace))]
    ;; Excecute the query and return the results
    (log/debug "search-apps-for-user::search_query:"
               (sql-only (select search_query)))
    (select search_query)))

(defn count-public-apps-by-user
  "Counts the number of apps integrated by a user."
  [email]
  ((comp :count first)
   (-> (select* analysis_listing)
       (aggregate (count :*) :count)
       (where {:deleted false})
       (add-public-apps-by-user-where-clause email)
       (select))))

(defn list-public-apps-by-user
  "Lists the apps integrated by the user with the given"
  [workspace favorites-group-index email query-opts]
  (-> (get-app-listing-base-query workspace favorites-group-index query-opts)
      (add-public-apps-by-user-where-clause email)
      (select)))
