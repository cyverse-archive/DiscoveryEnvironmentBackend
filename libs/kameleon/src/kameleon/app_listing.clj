(ns kameleon.app-listing
  (:use [korma.core :exclude [update]]
        [kameleon.entities]
        [kameleon.queries]
        [kameleon.util.search]
        [kameleon.app-groups :only [get-visible-root-app-group-ids]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn get-app-listing
  "Retrieves all app listing fields from the database for the given App ID."
  [app-id]
  (first (select app_listing (where {:id app-id}))))

(defn- get-all-group-ids-subselect
  "Gets a subselect that fetches the app_categories and its subgroup IDs with
   the stored procedure app_category_hierarchy_ids."
  [app_group_id]
  (subselect
    (sqlfn :app_category_hierarchy_ids app_group_id)))

(defn- get-fav-group-id-subselect
  "Gets a subselect that fetches the ID for the Favorites group at the given
   index under the app_categories with the given ID."
  [workspace_root_group_id favorites_group_index]
  (subselect
    :app_category_group
    (fields :child_category_id)
    (where {:parent_category_id workspace_root_group_id
            :child_index favorites_group_index})))

(defn- get-is-fav-sqlfn
  "Gets a sqlfn that retuns true if the App ID in its subselect is found in the
   Favorites group with the ID returned by get-fav-group-id-subselect."
  [workspace_root_group_id favorites_group_index]
  (let [fav_group_id_subselect (get-fav-group-id-subselect
                                 workspace_root_group_id
                                 favorites_group_index)]
    (sqlfn* :exists
            (subselect
              :app_category_app
              (where {:app_category_app.app_id
                      :app_listing.id})
              (where {:app_category_app.app_category_id
                      fav_group_id_subselect})))))

(defn- add-app-group-where-clause
  "Adds a where clause to an analysis listing query to restrict app results to
   an app group and all of its descendents."
  [base_listing_query app_group_id]
  (-> base_listing_query
    (join :app_category_app
          (= :app_category_app.app_id
             :app_listing.id))
    (where {:app_category_app.app_category_id
            [in (get-all-group-ids-subselect app_group_id)]})))

(defn- add-app-group-plus-public-apps-where-clause
  "Adds a where clause to an analysis listing query to restrict app results to
   an app group and all of its descendents plus the set of public apps
   integrated by a user."
  [query app-group-id email]
  (-> query
    (join :app_category_app
          (= :app_category_app.app_id
             :app_listing.id))
    (where (or {:app_category_app.app_category_id
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
   app_listing table."
  [query-opts]
  (->
    (select* app_listing)
    (fields (raw "count(DISTINCT app_listing.id) AS total"))
    (where {:deleted false})
    (add-agave-pipeline-where-clause query-opts)))

(defn count-apps-in-group-for-user
  "Counts all of the apps in an app group and all of its descendents."
  ([app-group-id query-opts]
    ((comp :total first)
      (-> (get-app-count-base-query query-opts)
        (add-app-group-where-clause app-group-id)
        (select))))
  ([app-group-id email query-opts]
    ((comp :total first)
      (-> (get-app-count-base-query query-opts)
        (add-app-group-plus-public-apps-where-clause app-group-id email)
        (select)))))

(defn- get-app-listing-base-query
  "Gets an app_listing select query, setting any query limits and sorting
   found in the query_opts, using the given workspace (as returned by
   fetch-workspace-by-user-id) to mark whether each app is a favorite and to
   include the user's rating in each app.."
  [workspace favorites_group_index query_opts]
  (let [user_id (:user_id workspace)
        workspace_root_group_id (:root_category_id workspace)
        row_offset (or (:offset query_opts) 0)
        row_limit (or (:limit query_opts) -1)
        sort_field (keyword (or (:sort-field query_opts) (:sortfield query_opts)))
        sort_dir (keyword (or (:sort-dir query_opts) (:sortdir query_opts)))

        ;; Bind the final query
        listing_query (->
                        (select* app_listing)
                        (modifier "DISTINCT")
                        (fields :id
                                :name
                                :lower_case_name
                                :description
                                :integrator_name
                                :integrator_email
                                :integration_date
                                :edited_date
                                :wiki_url
                                :average_rating
                                :total_ratings
                                :is_public
                                :step_count
                                :tool_count
                                :external_app_count
                                :task_count
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
                              (and (= :ratings.app_id
                                      :app_listing.id)
                                   (= :ratings.user_id
                                      user_id))))]

    ;; Add limits and sorting, if required, and return the query
    (->
      listing_query
      (add-agave-pipeline-where-clause query_opts)
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
  "Gets a subselect that fetches the workspace app_categories ID, public root
   group IDs, and their subgroup IDs with the stored procedure
   app_category_hierarchy_ids."
  [workspace_id]
  (let [root_app_ids (get-visible-root-app-group-ids workspace_id)
        select-ids-fn #(str "SELECT * FROM app_category_hierarchy_ids('" % "')")
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
            :tool_listing
            (where {:app_listing.id
                    :tool_listing.app_id})
            (where {(sqlfn lower :tool_listing.name)
                    [like (sqlfn lower search_term)]}))))

(defn- add-search-where-clauses
  "Adds where clauses to a base App search query to restrict results to Apps
   that contain search_term in their name or description, in all public groups
   and groups under the given workspace_id."
  [base_search_query search_term workspace_id]
  (let [search_term (format-query-wildcards search_term)
        search_term (str "%" search_term "%")
        sql-lower #(sqlfn lower %)]
    (->
      base_search_query
      (join :app_category_app
            (= :app_category_app.app_id
               :app_listing.id))
      (where {:app_category_app.app_category_id
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
  [search_term workspace_id params]
  (let [count_query (add-search-where-clauses
                      (get-app-count-base-query params)
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

(defn- add-deleted-and-orphaned-where-clause
  [query]
  (where query
    (or {:deleted true
         :is_public true}
      (raw "NOT EXISTS (SELECT * FROM app_category_app aca WHERE aca.app_id = app_listing.id)"))))

(defn count-deleted-and-orphaned-apps
  "Counts the number of deleted, public apps, plus apps that are not listed under any category."
  []
  ((comp :count first)
   (-> (select* app_listing)
       (aggregate (count :*) :count)
       add-deleted-and-orphaned-where-clause
       (select))))

(defn list-deleted-and-orphaned-apps
  "Fetches a list of deleted, public apps, plus apps that are not listed under any category."
  [{:keys [limit offset sort-field sort-dir]}]
  (-> (select* app_listing)
      add-deleted-and-orphaned-where-clause
      (add-query-limit limit)
      (add-query-offset offset)
      (add-query-sorting (when sort-field (keyword sort-field))
                         (when sort-dir (keyword (str/upper-case sort-dir))))
      (select)))

(defn count-public-apps-by-user
  "Counts the number of apps integrated by a user."
  [email params]
  ((comp :count first)
   (-> (select* app_listing)
       (aggregate (count :*) :count)
       (where {:deleted false})
       (add-public-apps-by-user-where-clause email)
       (add-agave-pipeline-where-clause params)
       (select))))

(defn list-public-apps-by-user
  "Lists the apps integrated by the user with the given"
  [workspace favorites-group-index email query-opts]
  (-> (get-app-listing-base-query workspace favorites-group-index query-opts)
      (add-public-apps-by-user-where-clause email)
      (select)))
