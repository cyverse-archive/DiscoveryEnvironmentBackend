(ns facepalm.c193-2014042401
  (:use [clojure.java.io :only [file reader]]
        [kameleon.sql-reader :only [sql-statements]]
        [korma.core]
        [korma.db :only [with-db]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [facepalm.core :as migrator]
            [kameleon.uuids :as uuids]
            [me.raynes.fs :as fs])
  (:import [java.util UUID]
           [java.util.regex Pattern]))

(def ^:private version
  "The destination database version."
  "1.9.3:20140424.01")

(defn exec-sql-statement
  "A wrapper around korma.core/exec-raw that logs the statement that is being
  executed if debugging is enabled."
  [& statements]
  (let [statement (clojure.string/join " " statements)]
    (log/debug "executing SQL statement:" statement)
    (exec-raw statement)))

(defn- load-sql-file
  "Loads a single SQL file into the database."
  [sql-file-path]
  (let [sql-file (fs/file sql-file-path)]
    (println (str "\t\t Loading " sql-file-path "..."))
    (with-open [rdr (reader sql-file)]
      (dorun (map exec-sql-statement (sql-statements rdr))))))

(defn- add-uuid-extension
  []
  (println "\t* adding uuid-ossp extension...")
  (with-db @migrator/admin-db-spec
    (load-sql-file "extensions/uuid.sql")))

;; Drop constraints
(defn- drop-all-constraints
  []
  (println "\t* dropping constraints...")
  (exec-sql-statement "
    DO $$DECLARE r record;
    BEGIN
      FOR r IN
        SELECT * FROM pg_constraint
        INNER JOIN pg_class ON conrelid=pg_class.oid
        INNER JOIN pg_namespace ON pg_namespace.oid=pg_class.relnamespace
        ORDER BY CASE WHEN contype='f' THEN 0 ELSE 1 END,contype,nspname,relname,conname
      LOOP
        EXECUTE 'ALTER TABLE ' || quote_ident(r.nspname) || '.' || quote_ident(r.relname) ||
                ' DROP CONSTRAINT ' || quote_ident(r.conname) || ';';
      END LOOP;
    END$$;")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/drop_constraints.sql"))

(defn- drop-views
  []
  (println "\t* dropping old views...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/drop_views.sql"))

(defn- add-agave-task
  [{:keys [id external_app_id]}]
  (println "\t* adding agave task for" external_app_id)
  (let [task-id (uuids/uuid)]
    (insert :tasks (values {:id task-id
                            :id_v192 (str task-id)
                            :name external_app_id
                            :external_app_id external_app_id}))
    (update :transformations_v192
            (set-fields {:template_id (subselect :tasks
                                                 (fields :id_v192)
                                                 (where {:id task-id}))})
            (where {:id id}))))

(defn- add-agave-step-tasks
  []
  (let [agave-steps (select :transformations_v192
                            (fields :id :external_app_id)
                            (where (raw "template_id IS NULL")))]
    (dorun (map add-agave-task agave-steps))))

(defn- get-unreferenced-dataobjects
  [join-table join-col]
  (select [join-table :t]
          (join [:file_parameters :f]
                {:f.hid_v192 join-col})
          (join [:parameters :p]
                {:p.dataobject_id_v192 :f.hid_v192})
          (fields [:t.template_id :task_id]
                  (raw "f.*"))
          (where (raw "p.dataobject_id_v192 IS NULL"))
          (order :t.template_id)
          (order :f.hid_v192)))

(defn- get-param-type-hid
  [param-type]
  ((comp :hid_v192 first)
   (select :parameter_types (fields :hid_v192) (where {:name param-type}))))

(defn- add-param-wrapper->group
  [group-id {:keys [hid_v192 display_order]}]
  (insert :property_group_property_v192 (values {:property_group_id group-id
                                                 :property_id hid_v192
                                                 :hid display_order})))

(defn- add-param-wrapper
  [index param]
  (insert :parameters (values (assoc param :display_order index))))

(defn- add-file-param-wrappers
  [task-id group-label params]
  (let [group-order ((comp inc :group_order first)
                     (select :template_property_group_v192
                             (aggregate (max :hid) :group_order)
                             (where {:template_id task-id})))
        group-id (:hid_v192 (insert :parameter_groups (values {:label group-label
                                                               :display_order group-order})))
        params (map-indexed add-param-wrapper params)]
    (insert :template_property_group_v192 (values {:template_id task-id
                                                   :property_group_id group-id
                                                   :hid group-order}))
    (dorun (map (partial add-param-wrapper->group group-id) params))))

(defn- input->param
  [param-type-id {:keys [hid_v192
                         name_v192
                         label_v192
                         description_v192
                         orderd_v192
                         switch_v192
                         required_v192]}]
  {:dataobject_id_v192 hid_v192
   :property_type_v192 param-type-id
   :name switch_v192
   :description description_v192
   :label name_v192
   :ordering orderd_v192
   :required required_v192
   :is_visible true})

(defn- output->param
  [param-type-id {:keys [is_implicit
                         hid_v192
                         name_v192
                         label_v192
                         description_v192
                         orderd_v192
                         switch_v192
                         required_v192]}]
  (let [visible (not is_implicit)]
    (when visible
      {:dataobject_id_v192 hid_v192
       :property_type_v192 param-type-id
       :name switch_v192
       :description description_v192
       :defalut_value_v192 name_v192
       :ordering orderd_v192
       :required required_v192
       :is_visible visible
       :omit_if_blank visible})))

(defn- add-unreferenced-inputs
  [param-type-id inputs]
  (let [task-id (:task_id (first inputs))
        params (remove nil? (map (partial input->param param-type-id) inputs))]
    (when-not (empty? params)
      (println "\t\t* adding unreferenced Inputs" params)
      (add-file-param-wrappers task-id "Inputs" params))))

(defn- add-unreferenced-outputs
  [param-type-id outputs]
  (let [task-id (:task_id (first outputs))
        params (remove nil? (map (partial output->param param-type-id) outputs))]
    (when-not (empty? params)
      (println "\t\t* adding unreferenced Outputs" params)
      (add-file-param-wrappers task-id "Outputs" params))))

(defn- convert-unreferenced-file-params
  []
  (let [inputs (group-by :task_id (get-unreferenced-dataobjects :template_input_v192 :t.input_id))
        outputs (group-by :task_id (get-unreferenced-dataobjects :template_output_v192 :t.output_id))
        input-param-type-id (get-param-type-hid "Input")
        output-param-type-id (get-param-type-hid "Output")]
    (when-not (empty? inputs)
      (println "\t\t* adding unreferenced Inputs for task" (ffirst inputs))
      (dorun (map (partial add-unreferenced-inputs input-param-type-id) (vals inputs))))
    (when-not (empty? outputs)
      (println "\t\t* adding unreferenced Outputs for task" (ffirst outputs))
      (dorun (map (partial add-unreferenced-outputs output-param-type-id) (vals outputs))))))

;; Rename or add new tables and columns.
(defn- run-table-conversions
  "Loads and runs SQL files containing table and column conversions."
  []
  (println "\t* renaming obsolete tables")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/rename_obsolete_tables.sql")
  (println "\t* updating the template_group table to app_categories")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/01_app_categories.sql")
  (println "\t* updating the workspace table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/02_workspace.sql")
  (println "\t* updating the deployed_components table to tools")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/03_tools.sql")
  (println "\t* updating the template table to tasks")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/04_tasks.sql")
  (println "\t* adding agave step tasks")
  (add-agave-step-tasks)
  (println "\t* updating the transformation_activity table to apps")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/05_apps.sql")
  (println "\t* updating the transformation_task_steps table to app_steps")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/07_app_steps.sql")
  (println "\t* updating the integration_data table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/09_integration_data.sql")
  (println "\t* updating the ratings table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/10_ratings.sql")
  (println "\t* updating the template_group_template table to app_category_app")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/11_app_category_app.sql")
  (println "\t* updating the data_formats table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/12_data_formats.sql")
  (println "\t* updating the input_output_mapping table to workflow_io_maps")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/13_workflow_io_maps.sql")
  (println "\t* updating the dataobject_mapping table to input_output_mapping")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/18_input_output_mapping.sql")
  (println "\t* updating the dataobjects table to file_parameters")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/14_file_parameters.sql")
  (println "\t* updating the deployed_component_data_files table to tool_test_data_files")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/15_tool_test_data_files.sql")
  (println "\t* updating the info_type table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/17_info_type.sql")
  (println "\t* updating the property table to parameters")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/24_parameters.sql")
  (println "\t* updating the property_group table to parameter_groups")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/25_parameter_groups.sql")
  (println "\t* adding the parameter_values table")
  (load-sql-file "tables/25_parameter_values.sql")
  (println "\t* updating the property_type table to parameter_types")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/27_parameter_types.sql")
  (println "\t* updating the users table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/28_users.sql")
  (println "\t* updating the rule table to validation_rules")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/29_validation_rules.sql")
  (println "\t* updating the rule_argument table to validation_rule_arguments")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/30_validation_rule_arguments.sql")
  (println "\t* updating the rule_subtype table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/31_rule_subtype.sql")
  (println "\t* updating the rule_type table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/32_rule_type.sql")
  (println "\t* updating the rule_type_value_type table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/33_rule_type_value_type.sql")
  (println "\t* updating the suggested_groups table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/34_suggested_groups.sql")
  (println "\t* updating the template_group_group table to app_category_group")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/35_app_category_group.sql")
  (println "\t* updating the transformation_activity_references table to app_references")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/40_app_references.sql")
  (println "\t* updating the value_type table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/44_value_type.sql")
  (println "\t* updating the version table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/45_version.sql")
  (println "\t* updating the genome_reference table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/46_genome_reference.sql")
  (println "\t* updating the collaborators table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/47_collaborators.sql")
  (println "\t* updating the data_source table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/48_data_source.sql")
  (println "\t* updating the tool_types table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/49_tool_types.sql")
  (println "\t* updating the tool_type_property_type table to tool_type_parameter_type")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/50_tool_type_parameter_type.sql")
  (println "\t* updating the tool_request_status_codes table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/51_tool_request_status_codes.sql")
  (println "\t* updating the tool_architectures table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/52_tool_architectures.sql")
  (println "\t* updating the tool_requests table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/53_tool_requests.sql")
  (println "\t* updating the tool_request_statuses table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/54_tool_request_statuses.sql")
  (println "\t* updating the logins table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/55_logins.sql")
  (println "\t* updating the job_types table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/56_job_types.sql")
  (println "\t* updating the jobs table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/57_jobs.sql")
  (println "\t* updating the metadata_value_types table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/58_metadata_value_types.sql")
  (println "\t* updating the metadata_templates table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/59_metadata_templates.sql")
  (println "\t* updating the metadata_attributes table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/60_metadata_attributes.sql")
  (println "\t* updating the user_preferences table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/63_user_preferences.sql")
  (println "\t* updating the user_sessions table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/64_user_sessions.sql")
  (println "\t* updating the tree_urls table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/65_tree_urls.sql")
  (println "\t* updating the user_saved_searches table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/66_user_saved_searches.sql")
  (println "\t* updating the access_tokens table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/67_access_tokens.sql")
  (println "\t* updating the authorization_requests table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/68_authorization_requests.sql")
  (println "\t* updating the job_steps table")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/69_job_steps.sql"))

(defn- add-unreferenced-file-params
  []
  (println "\t* adding unreferenced inputs/outputs as file parameters")
  (convert-unreferenced-file-params))

;; Update new UUID columns.
(defn- run-uuid-conversions
  []
  (println "\t* updating app_categories uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/01_app_categories.sql")
  (println "\t* updating workspace uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/02_workspace.sql")
  (println "\t* updating tools uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/03_tools.sql")
  (println "\t* updating tasks uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/04_tasks.sql")
  (println "\t* updating apps uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/05_apps.sql")
  (println "\t* updating app_steps uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/07_app_steps.sql")
  (println "\t* updating integration_data uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/09_integration_data.sql")
  (println "\t* updating data_formats uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/12_data_formats.sql")
  (println "\t* updating workflow_io_maps uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/13_workflow_io_maps.sql")
  (println "\t* updating info_type uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/17_info_type.sql")
  (println "\t* updating parameters uuid foreign keys (this might take a minute or 2)...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/24_parameters.sql")
  (println "\t* updating parameter_groups uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/25_parameter_groups.sql")
  (println "\t* updating parameter_types uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/27_parameter_types.sql")
  (println "\t* updating users uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/28_users.sql")
  (println "\t* updating validation_rules uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/29_validation_rules.sql")
  (println "\t* updating rule_subtype uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/31_rule_subtype.sql")
  (println "\t* updating rule_type uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/32_rule_type.sql")
  (println "\t* updating value_type uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/44_value_type.sql")
  (println "\t* updating data_source uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/48_data_source.sql")
  (println "\t* updating tool_types uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/49_tool_types.sql")
  (println "\t* updating tool_architectures uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/52_tool_architectures.sql")
  (println "\t* updating tool_requests uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/53_tool_requests.sql")
  (println "\t* updating job_types uuid foreign keys...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/56_job_types.sql")
  (println "\t* updating jobs uuid foreign keys")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/uuids/57_jobs.sql"))

(defn- drop-obsolete-tables
  []
  (println "\t* dropping empty obsolete tables")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/tables/drop_empty_obsolete_tables.sql"))

(defn- re-add-constraints
  []
  (println "\t* re-adding constraints")
  (load-sql-file "tables/99_constraints.sql"))

(defn- add-new-views
  []
  (println "\t* adding app_category_listing view...")
  (load-sql-file "views/01_app_category_listing.sql")
  (println "\t* adding app_job_types view...")
  (load-sql-file "views/02_app_job_types.sql")
  (println "\t* adding app_listing view...")
  (load-sql-file "views/03_app_listing.sql")
  (println "\t* adding tool_listing view...")
  (load-sql-file "views/04_tool_listing.sql")
  (println "\t* adding rating_listing view...")
  (load-sql-file "views/05_rating_listing.sql")
  (println "\t* adding job_listing view...")
  (load-sql-file "views/06_job_listing.sql")
  (println "\t* adding task_param_listing view...")
  (load-sql-file "views/07_task_param_listing.sql"))

(defn- reload-functions
  []
  (println "\t* adding app_category_hierarchy_ids function...")
  (exec-raw "DROP FUNCTION IF EXISTS app_group_hierarchy_ids(bigint)")
  (load-sql-file "functions/01_app_category_hierarchy_ids.sql")
  (println "\t* reloading app_count function...")
  (exec-raw "DROP FUNCTION IF EXISTS app_count(bigint)")
  (exec-raw "DROP FUNCTION IF EXISTS app_count(bigint, boolean)")
  (load-sql-file "functions/02_app_count.sql")
  (println "\t* adding app_category_hierarchy function...")
  (exec-raw "DROP FUNCTION IF EXISTS analysis_group_hierarchy(bigint)")
  (exec-raw "DROP FUNCTION IF EXISTS analysis_group_hierarchy(bigint, boolean)")
  (load-sql-file "functions/03_app_category_hierarchy.sql"))

(defn- add-new-data
  []
  (println "\t* adding new reference genome parameter types...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/data/01_parameter_types.sql")
  (println "\t* adding new tool types...")
  (load-sql-file "conversions/v1.9.3/c193_2014042401/data/02_tool_types.sql")
  (println "\t* adding internal apps...")
  (load-sql-file "data/21_internal_apps.sql"))

(defn- param-type-subselect
  [param-type]
  (subselect :parameter_types
             (fields :id)
             (where {:name param-type})))

(defn- info-type-subselect
  []
  (subselect [:file_parameters :fp]
             (fields :i.name)
             (join [:info_type :i] {:fp.info_type :i.id})
             (where {:parameters.id :fp.parameter_id})))

(defn- multiplicity-subselect
  []
  (subselect [:file_parameters :fp]
             (fields :m.name)
             (join [:multiplicity_v192 :m] {:fp.multiplicity_v192 :m.hid})
             (where {:parameters.id :fp.parameter_id})))

(defn- convert-reference-genome-parameters
  [ref-gen-type]
  (println "\t* converting" ref-gen-type "parameters")
  (update :parameters
          (set-fields {:parameter_type (param-type-subselect ref-gen-type)})
          (where (and (= :parameter_type (param-type-subselect "Input"))
                      (= ref-gen-type (info-type-subselect))))))

(defn- convert-parameter-types
  [new-param-type old-param-type old-multiplicity]
  (println "\t* converting" new-param-type "parameters")
  (update :parameters
          (set-fields {:parameter_type (param-type-subselect new-param-type)})
          (where (and (= :parameter_type (param-type-subselect old-param-type))
                      (= old-multiplicity (multiplicity-subselect))))))

(defn- list-de-jobs
  []
  (exec-raw
   "SELECT j.id AS \"job-id\",
           j.app_id AS \"new-id\",
           a.id_v192 AS \"old-id\",
           j.submission AS \"submission\"
    FROM jobs j
    LEFT JOIN apps a ON j.app_id = CAST(a.id AS character varying)"
   :results))

(defn- build-step-name-id-map
  [steps]
  (into {} (map (juxt :step-name (comp str :step-id)) steps)))

(defn- load-step-name-id-maps
  []
  (->> (select [:app_steps :s]
               (join :inner
                     [:transformation_steps_v192 :ts]
                     {:s.transformation_step_id_v192 :ts.id})
               (fields [:s.app_id :app-id] [:ts.name :step-name] [:s.id :step-id]))
       (group-by :app-id)
       (map (fn [[app-id steps]] [(str app-id) (build-step-name-id-map steps)]))
       (into {})))

(defn- build-param-id-map
  [params]
  (into {} (map (juxt :old-id (comp str :new-id)) params)))

(defn- load-param-id-maps
  []
  (->> (select [:app_steps :s]
               (join :inner [:tasks :t] {:s.task_id :t.id})
               (join :inner [:parameter_groups :pg] {:t.id :pg.task_id})
               (join :inner [:parameters :p] {:pg.id :p.parameter_group_id})
               (fields [:s.app_id :app-id] [:p.id_v192 :old-id] [:p.id :new-id]))
       (group-by :app-id)
       (map (fn [[app-id params]] [(str app-id) (build-param-id-map params)]))
       (into {})))

(defn- config-key-pattern
  [name-id-map]
  (let [name-pattern (string/join "|" (map #(Pattern/quote %) (keys name-id-map)))]
    (re-pattern (str "\\A(" name-pattern ")_(.*)\\z"))))

(defn- fix-config-key*
  [key-pattern name-id-map param-id-map k]
  (string/replace (name k) key-pattern
                  (fn [[_ step-name param-id]]
                    (str (or (name-id-map step-name) step-name)
                         "_"
                         (or (param-id-map param-id) param-id)))))
(defn- fix-config-key
  [key-pattern name-id-map param-id-map [k v]]
  [(keyword (fix-config-key* key-pattern name-id-map param-id-map k)) v])

(defn- fix-submission
  [old-id new-id new-config submission]
  (-> (assoc submission
        :app_id          (or new-id old-id)
        :app_name        (:analysis_name submission)
        :app_description (:analysis_details submission)
        :config          new-config)
      (dissoc :analysis_id :analysis_name :analysis_details)))

(defn- remove-vals
  [pred m]
  (into {} (remove (comp pred val) m)))

(defn- update-job-submission
  [step-name-id-maps param-id-maps {:keys [job-id new-id old-id submission]}]
  (when-not (nil? submission)
    (let [submission   (cheshire/decode (.getValue submission) true)
          name-id-map  (step-name-id-maps new-id {})
          key-pattern  (config-key-pattern name-id-map)
          param-id-map (param-id-maps new-id {})
          fix-key      (partial fix-config-key key-pattern name-id-map param-id-map)
          old-config   (:config submission)
          new-config   (when-not (nil? old-config) (into {} (map fix-key old-config)))
          submission   (remove-vals nil? (fix-submission old-id new-id new-config submission))]
      (exec-raw ["UPDATE jobs SET submission = CAST ( ? AS json ) WHERE id = ?"
                 [(cast Object (cheshire/encode submission)) job-id]]))))

(defn- convert-job-submissions
  []
  (println "\t* converting job submissions--this could take a while")
  (let [step-name-id-maps (load-step-name-id-maps)
        param-id-maps     (load-param-id-maps)]
    (dorun (map (partial update-job-submission step-name-id-maps param-id-maps)
                (list-de-jobs)))))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-uuid-extension)
  (drop-views)
  (run-table-conversions)
  (add-unreferenced-file-params)
  (run-uuid-conversions)
  (drop-all-constraints)
  (drop-obsolete-tables)
  (re-add-constraints)
  (add-new-views)
  (reload-functions)
  (add-new-data)
  (convert-reference-genome-parameters "ReferenceGenome")
  (convert-reference-genome-parameters "ReferenceSequence")
  (convert-reference-genome-parameters "ReferenceAnnotation")
  (convert-parameter-types "FileInput" "Input" "single")
  (convert-parameter-types "FolderInput" "Input" "collection")
  (convert-parameter-types "MultiFileSelector" "Input" "many")
  (convert-parameter-types "FileOutput" "Output" "single")
  (convert-parameter-types "FolderOutput" "Output" "collection")
  (convert-parameter-types "MultiFileOutput" "Output" "many")
  (convert-job-submissions))
