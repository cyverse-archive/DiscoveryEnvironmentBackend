(ns facepalm.c200-2014042401
  (:use [clojure.java.io :only [file reader]]
        [kameleon.sql-reader :only [sql-statements]]
        [korma.core])
  (:require [clojure.tools.logging :as log]
            [me.raynes.fs :as fs])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "2.0.0:20140424.01")

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
    (println (str "Loading " (.getName sql-file) "..."))
    (with-open [rdr (reader sql-file)]
      (dorun (map exec-sql-statement (sql-statements rdr))))))

;; Drop constraints
(defn- drop-all-constraints
  []
  (println "\t* droping constraints...")
  (exec-sql-statement
   "DO $body$"
   " DECLARE r record;"
   " BEGIN"
   "  FOR r IN"
   "   SELECT * FROM pg_constraint"
   "   INNER JOIN pg_class ON conrelid=pg_class.oid"
   "   INNER JOIN pg_namespace ON pg_namespace.oid=pg_class.relnamespace"
   "   ORDER BY CASE WHEN contype='f' THEN 0 ELSE 1 END,contype,nspname,relname,conname"
   "  LOOP"
   "   EXECUTE 'ALTER TABLE ' || quote_ident(r.nspname) || '.' || quote_ident(r.relname) ||"
   "           ' DROP CONSTRAINT ' || quote_ident(r.conname) || ';';"
   "  END LOOP;"
   " END"
   "$body$;"))

;; TODO add NOT NULL contraints to FOREIGN KEY cols.

;; Dropped sequences
;; template_group_id_seq
;; workspace_id_seq
;; deployed_component_id_seq
;; template_id_seq
;; transformation_activity_id_seq
;; transformation_steps_id_seq
;; transformations_id_seq
;; integration_data_id_seq
;; ratings_id_seq
;; data_formats_id_seq
;; dataobjects_id_seq
;; deployed_component_data_files_id_seq
;; info_type_id_seq
;; input_output_mapping_id_seq
;; multiplicity_id_seq
;; notification_id_seq
;; notification_set_id_seq
;; property_id_seq
;; property_group_id_seq
;; property_type_id_seq
;; users_id_seq
;; rule_id_seq
;; rule_subtype_id_seq
;; rule_type_id_seq
;; transformation_activity_references_id_seq
;; transformation_values_id_seq
;; validator_id_seq
;; value_type_id_seq
;; version_id_seq;
;; genome_ref_id_seq;
;; collaborators_id_seq;
;; data_source_id_seq;
;; tool_types_id_seq
;; tool_architectures_id_seq
;; tool_requests_id_seq
;; tool_request_statuses_id_seq
;; tool_request_status_codes_id_seq
;; job_types_id_seq

;; Dropped tables
;; 06_transformation_steps
;; 08_transformations
;; 16_hibernate_sequence
;; 20_notification
;; 21_notification_set
;; 22_notification_set_notification
;; 23_notifications_receivers
;; 26_property_group_property
;; 36_template_input
;; 37_template_output
;; 38_template_property_group
;; 39_transformation_activity_mappings
;; 41_transformation_values
;; 42_validator
;; 43_validator_rule

(defn- drop-views
  "Drops the old views."
  []
  (println "\t* droping views...")
  (exec-sql-statement "DROP VIEW analysis_group_listing")
  (exec-sql-statement "DROP VIEW analysis_job_types")
  (exec-sql-statement "DROP VIEW analysis_listing")
  (exec-sql-statement "DROP VIEW deployed_component_listing")
  (exec-sql-statement "DROP VIEW rating_listing"))

(defn- run-table-conversions
  "Loads and runs SQL files containing table and column conversions."
  []
  (println "\t* updating the template_group table to app_categories")
  (load-sql-file "conversions/c200_2014042401/tables/01_app_categories.sql")
  (println "\t* updating the workspace table")
  (load-sql-file "conversions/c200_2014042401/tables/02_workspace.sql")
  (println "\t* updating the deployed_components table to tools")
  (load-sql-file "conversions/c200_2014042401/tables/03_tools.sql")
  (println "\t* updating the template table to tasks")
  (load-sql-file "conversions/c200_2014042401/tables/04_tasks.sql")
  (println "\t* updating the transformation_activity table to apps")
  (load-sql-file "conversions/c200_2014042401/tables/05_apps.sql")
  (println "\t* updating the transformation_task_steps table to app_steps")
  (load-sql-file "conversions/c200_2014042401/tables/07_app_steps.sql")
  (println "\t* updating the integration_data table")
  (load-sql-file "conversions/c200_2014042401/tables/09_integration_data.sql")
  (println "\t* updating the ratings table")
  (load-sql-file "conversions/c200_2014042401/tables/10_ratings.sql")
  (println "\t* updating the template_group_template table to app_category_app")
  (load-sql-file "conversions/c200_2014042401/tables/11_app_category_app.sql")
  (println "\t* updating the data_formats table")
  (load-sql-file "conversions/c200_2014042401/tables/12_data_formats.sql")
  (println "\t* updating the input_output_mapping table to workflow_io_maps")
  (load-sql-file "conversions/c200_2014042401/tables/13_workflow_io_maps.sql")
  (println "\t* updating the dataobject_mapping table to input_output_mapping")
  (load-sql-file "conversions/c200_2014042401/tables/18_input_output_mapping.sql")
  (println "\t* updating the dataobjects table to file_parameters")
  (load-sql-file "conversions/c200_2014042401/tables/14_file_parameters.sql")
  (println "\t* updating the deployed_component_data_files table to tool_test_data_files")
  (load-sql-file "conversions/c200_2014042401/tables/15_tool_test_data_files.sql")
  (println "\t* updating the info_type table")
  (load-sql-file "conversions/c200_2014042401/tables/17_info_type.sql")
  (println "\t* updating the multiplicity table")
  (load-sql-file "conversions/c200_2014042401/tables/19_multiplicity.sql")
  (println "\t* updating the property table to parameters")
  (load-sql-file "conversions/c200_2014042401/tables/24_parameters.sql")
  (println "\t* updating the property_group table to parameter_groups")
  (load-sql-file "conversions/c200_2014042401/tables/25_parameter_groups.sql")
  (println "\t* adding the parameter_values table")
  (load-sql-file "tables/25_parameter_values.sql")
  (println "\t* updating the property_type table to parameter_types")
  (load-sql-file "conversions/c200_2014042401/tables/27_parameter_types.sql")
  (println "\t* updating the users table")
  (load-sql-file "conversions/c200_2014042401/tables/28_users.sql")
  (println "\t* updating the rule table to validation_rules")
  (load-sql-file "conversions/c200_2014042401/tables/29_validation_rules.sql")
  (println "\t* updating the rule_argument table to validation_rule_arguments")
  (load-sql-file "conversions/c200_2014042401/tables/30_validation_rule_arguments.sql")
  (println "\t* updating the rule_subtype table")
  (load-sql-file "conversions/c200_2014042401/tables/31_rule_subtype.sql")
  (println "\t* updating the rule_type table")
  (load-sql-file "conversions/c200_2014042401/tables/32_rule_type.sql")
  (println "\t* updating the rule_type_value_type table")
  (load-sql-file "conversions/c200_2014042401/tables/33_rule_type_value_type.sql")
  (println "\t* updating the suggested_groups table")
  (load-sql-file "conversions/c200_2014042401/tables/34_suggested_groups.sql")
  (println "\t* updating the template_group_group table to app_category_group")
  (load-sql-file "conversions/c200_2014042401/tables/35_app_category_group.sql")
  (println "\t* updating the transformation_activity_references table to app_references")
  (load-sql-file "conversions/c200_2014042401/tables/40_app_references.sql")
  (println "\t* updating the value_type table")
  (load-sql-file "conversions/c200_2014042401/tables/44_value_type.sql")
  (println "\t* updating the version table")
  (load-sql-file "conversions/c200_2014042401/tables/45_version.sql")
  (println "\t* updating the genome_reference table")
  (load-sql-file "conversions/c200_2014042401/tables/46_genome_reference.sql")
  (println "\t* updating the collaborators table")
  (load-sql-file "conversions/c200_2014042401/tables/47_collaborators.sql")
  (println "\t* updating the data_source table")
  (load-sql-file "conversions/c200_2014042401/tables/48_data_source.sql")
  (println "\t* updating the tool_types table")
  (load-sql-file "conversions/c200_2014042401/tables/49_tool_types.sql")
  (println "\t* updating the tool_type_property_type table to tool_type_parameter_type")
  (load-sql-file "conversions/c200_2014042401/tables/50_tool_type_parameter_type.sql")
  (println "\t* updating the tool_architectures table")
  (load-sql-file "conversions/c200_2014042401/tables/52_tool_architectures.sql")
  (println "\t* updating the tool_requests table")
  (load-sql-file "conversions/c200_2014042401/tables/53_tool_requests.sql")
  (println "\t* updating the tool_request_statuses table")
  (load-sql-file "conversions/c200_2014042401/tables/54_tool_request_statuses.sql")
  (println "\t* updating the logins table")
  (load-sql-file "conversions/c200_2014042401/tables/55_logins.sql")
  (println "\t* updating the job_types table")
  (load-sql-file "conversions/c200_2014042401/tables/56_job_types.sql")
  (println "\t* updating the jobs table")
  (load-sql-file "conversions/c200_2014042401/tables/57_jobs.sql")
  (println "\t* updating the user_preferences table")
  (load-sql-file "conversions/c200_2014042401/tables/63_user_preferences.sql")
  (println "\t* updating the user_sessions table")
  (load-sql-file "conversions/c200_2014042401/tables/64_user_sessions.sql")
  (println "\t* updating the user_saved_searches table")
  (load-sql-file "conversions/c200_2014042401/tables/66_user_saved_searches.sql"))


;; Update new UUID columns.

(defn- update-app-category-uuids
  []
  (println "\t* updating app_categories uuid foreign keys...")
  (exec-sql-statement "UPDATE workspace SET root_category_id ="
                      "(SELECT ac.id FROM app_categories ac WHERE root_analysis_group_id = ac.hid)")
  (exec-sql-statement "UPDATE app_category_app SET app_category_id ="
                      "(SELECT ac.id FROM app_categories ac WHERE template_group_id = ac.hid)")
  (exec-sql-statement "UPDATE suggested_groups SET app_category_id ="
                      "(SELECT ac.id FROM app_categories ac WHERE template_group_id = ac.hid)")
  (exec-sql-statement "UPDATE app_category_group SET parent_category_id ="
                      "(SELECT ac.id FROM app_categories ac WHERE parent_group_id = ac.hid)")
  (exec-sql-statement "UPDATE app_category_group SET child_category_id ="
                      "(SELECT ac.id FROM app_categories ac WHERE subgroup_id = ac.hid)"))

(defn- update-workspace-uuids
  []
  (println "\t* updating workspace uuid foreign keys...")
  (exec-sql-statement "UPDATE app_categories SET workspace_id ="
                      "(SELECT w.id FROM workspace w WHERE workspace_id_v187 = w.id_v187)"))

(defn- update-tool-uuids
  []
  (println "\t* updating tools uuid foreign keys...")
  (exec-sql-statement "UPDATE tasks SET tool_id ="
                      "(SELECT t.id FROM tools t WHERE component_id = t.id_v187)")
  (exec-sql-statement "UPDATE tool_test_data_files SET tool_id ="
                      "(SELECT t.id FROM tools t WHERE deployed_component_id = t.hid)")
  (exec-sql-statement "UPDATE tool_requests SET tool_id ="
                      "(SELECT t.id FROM tools t WHERE deployed_component_id = t.hid)"))

(defn- update-task-uuids
  []
  (println "\t* updating tasks uuid foreign keys...")
  ;; Add temporary indexes to help speed up the conversion.
  (exec-sql-statement "CREATE INDEX tasks_id_v187_idx ON tasks(id_v187)")
  (exec-sql-statement "CREATE INDEX app_steps_transformation_step_id_idx ON app_steps(transformation_step_id)")
  (exec-sql-statement "CREATE INDEX transformations_template_id_idx ON transformations(template_id)")
  (exec-sql-statement "CREATE INDEX transformation_steps_transformation_id_idx ON transformation_steps(transformation_id)")
  (exec-sql-statement "CREATE INDEX template_property_group_property_group_id_idx ON template_property_group(property_group_id)")
  (exec-sql-statement "UPDATE app_steps SET task_id ="
                      "(SELECT t.id FROM tasks t"
                      " LEFT JOIN transformations tx ON tx.template_id = t.id_v187"
                      " LEFT JOIN transformation_steps ts ON ts.transformation_id = tx.id"
                      " WHERE transformation_step_id = ts.id)")
  (exec-sql-statement "UPDATE parameter_groups SET task_id ="
                      "(SELECT t.id FROM tasks t"
                      " LEFT JOIN template_property_group tgt ON tgt.template_id = t.hid"
                      " WHERE property_group_id = parameter_groups.hid)")
  ;; Drop temporary indexes.
  (exec-sql-statement "DROP INDEX tasks_id_v187_idx")
  (exec-sql-statement "DROP INDEX app_steps_transformation_step_id_idx")
  (exec-sql-statement "DROP INDEX transformations_template_id_idx")
  (exec-sql-statement "DROP INDEX transformation_steps_transformation_id_idx")
  (exec-sql-statement "DROP INDEX template_property_group_property_group_id_idx"))

;; workflow_io_maps needs to be done after its other cols have been populated.
(defn- update-app-uuids
  []
  (println "\t* updating apps uuid foreign keys...")
  (exec-sql-statement "UPDATE app_steps SET app_id ="
                      "(SELECT a.id FROM apps a WHERE transformation_task_id = a.hid)")
  (exec-sql-statement "UPDATE ratings SET app_id ="
                      "(SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid)")
  (exec-sql-statement "UPDATE app_category_app SET app_id ="
                      "(SELECT a.id FROM apps a WHERE template_id = a.hid)")
  (exec-sql-statement "UPDATE workflow_io_maps m SET app_id ="
                      "(SELECT a.id FROM apps a"
                      " LEFT JOIN transformation_activity_mappings tm"
                      " ON tm.transformation_activity_id = a.hid"
                      " WHERE m.hid = tm.mapping_id)")
  (exec-sql-statement "UPDATE suggested_groups SET app_id ="
                      "(SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid)")
  (exec-sql-statement "UPDATE app_references SET app_id ="
                      "(SELECT a.id FROM apps a WHERE transformation_activity_id = a.hid)"))

(defn- update-app-steps-uuids
  []
  (println "\t* updating app_steps uuid foreign keys...")
  (exec-sql-statement "UPDATE workflow_io_maps SET source_step ="
                      "(SELECT step.id FROM app_steps step"
                      " LEFT JOIN transformation_steps ts ON ts.id = step.transformation_step_id"
                      " WHERE source = ts.id)")
  (exec-sql-statement "UPDATE workflow_io_maps SET target_step ="
                      "(SELECT step.id FROM app_steps step"
                      " LEFT JOIN transformation_steps ts ON ts.id = step.transformation_step_id"
                      " WHERE target = ts.id)"))

(defn- update-integration-data-uuids
  []
  (println "\t* updating integration_data uuid foreign keys...")
  (exec-sql-statement "UPDATE tools SET integration_data_id ="
                      "(SELECT i.id FROM integration_data i WHERE integration_data_id_v187 = i.id_v187)")
  (exec-sql-statement "UPDATE apps SET integration_data_id ="
                      "(SELECT i.id FROM integration_data i WHERE integration_data_id_v187 = i.id_v187)"))

(defn- update-data-formats-uuids
  []
  (println "\t* updating data_formats uuid foreign keys...")
  (exec-sql-statement "UPDATE file_parameters SET data_format ="
                      "(SELECT d.id FROM data_formats d WHERE data_format_v187 = d.id_v187)"))

(defn- update-workflow-io-maps-uuids
  []
  (println "\t* updating workflow_io_maps uuid foreign keys...")
  (exec-sql-statement "UPDATE input_output_mapping SET mapping_id ="
                      "(SELECT id FROM workflow_io_maps"
                      " WHERE hid = mapping_id_v187)"))

(defn- update-file-parameters-uuids
  []
  (println "\t* updating file_parameters uuid foreign keys...")
  (exec-sql-statement "UPDATE parameters SET file_parameter_id ="
                      "(SELECT id FROM file_parameters"
                      " WHERE hid = dataobject_id)")
  (exec-sql-statement "UPDATE input_output_mapping SET input ="
                      "(SELECT id FROM file_parameters"
                      " WHERE id_v187 = input_v187)")
  (exec-sql-statement "UPDATE input_output_mapping SET output ="
                      "(SELECT id FROM file_parameters"
                      " WHERE id_v187 = output_v187)")
  (exec-sql-statement "DELETE FROM input_output_mapping WHERE input IS NULL OR output IS NULL"))

(defn- re-add-constraints
  []
  (println "\t* re-adding constraints")
  (load-sql-file "tables/99_constraints.sql"))

(defn- add-app-category-listing-view
  []
  (println "\t* adding app_category_listing view...")
  (load-sql-file "views/01_app_category_listing.sql"))

(defn- add-app-job-types-view
  []
  (println "\t* adding app_job_types view...")
  (load-sql-file "views/02_app_job_types.sql"))

(defn- add-app-listing-view
  []
  (println "\t* adding app_listing view...")
  (load-sql-file "views/03_app_listing.sql"))

(defn- add-tool-listing-view
  []
  (println "\t* adding tool_listing view...")
  (load-sql-file "views/04_tool_listing.sql"))

(defn- add-rating-listing-view
  []
  (println "\t* adding rating_listing view...")
  (load-sql-file "views/05_rating_listing.sql"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (drop-views)
  (run-table-conversions)
  (update-app-category-uuids)
  (update-workspace-uuids)
  (update-tool-uuids)
  (update-task-uuids)
  (update-app-uuids)
  (update-app-steps-uuids)
  (update-integration-data-uuids)
  (update-data-formats-uuids)
  (update-workflow-io-maps-uuids)
  (update-file-parameters-uuids)
  (drop-all-constraints)
  (re-add-constraints)
  (add-app-category-listing-view)
  (add-app-job-types-view)
  (add-app-listing-view)
  (add-tool-listing-view)
  (add-rating-listing-view))
