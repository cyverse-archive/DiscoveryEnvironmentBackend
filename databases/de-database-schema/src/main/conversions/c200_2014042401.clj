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

(defn- alter-id-column-to-uuid
  [table]
  (exec-sql-statement "ALTER TABLE ONLY "
                      table
                      "ALTER COLUMN id TYPE UUID USING"
                      "CASE WHEN CHAR_LENGTH(id) < 36 THEN (uuid_generate_v4())"
                      "ELSE CAST(id AS UUID) END"))

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

;; cols to drop: hid, workspace_id_v187
(defn- add-app-categories-table
  "Renames the existing template_group table to app_categories and adds updated columns."
  []
  (println "\t* updating the template_group table to app_categories")
  (exec-sql-statement "ALTER TABLE template_group RENAME TO app_categories")
  (exec-sql-statement "UPDATE app_categories SET id = '12c7a585-ec23-3352-e313-02e323112a7c' WHERE id = 'g12c7a585ec233352e31302e323112a7ccf18bfd7364'")
  (exec-sql-statement "UPDATE app_categories SET id = '5401bd146c144470aedd57b47ea1b979' WHERE id = 'g5401bd146c144470aedd57b47ea1b979'")
  (exec-sql-statement "ALTER TABLE ONLY app_categories ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)")
  (exec-sql-statement "ALTER TABLE ONLY app_categories RENAME COLUMN workspace_id TO workspace_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY app_categories ADD COLUMN workspace_id UUID"))

;; cols to drop: id_v187, root_analysis_group_id, user_id_v187
(defn- alter-workspace-table
  "Updates columns in the existing workspace table."
  []
  (println "\t* updating the workspace table")
  (exec-sql-statement "ALTER TABLE ONLY workspace RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY workspace ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY workspace ADD COLUMN root_category_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY workspace RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY workspace ADD COLUMN user_id UUID"))

;; cols to drop: hid, id_v187, tool_type_id_v187, integration_data_id_v187
(defn- add-tools-table
  "Renames the existing deployed_components table to tools and adds updated columns."
  []
  (println "\t* updating the deployed_components table to tools")
  (exec-sql-statement "ALTER TABLE deployed_components RENAME TO tools")
  (exec-sql-statement "ALTER TABLE ONLY tools RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tools ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "UPDATE tools SET id = CAST(id_v187 AS UUID) WHERE CHAR_LENGTH(id_v187) = 36")
  (exec-sql-statement "ALTER TABLE ONLY tools RENAME COLUMN tool_type_id TO tool_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tools ADD COLUMN tool_type_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY tools RENAME COLUMN integration_data_id TO integration_data_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tools ADD COLUMN integration_data_id UUID"))

;; cols to drop: hid, component_id
(defn- add-tasks-table
  "Renames the existing template table to tasks and adds updated columns."
  []
  (println "\t* updating the template table to tasks")
  (exec-sql-statement "ALTER TABLE template RENAME TO tasks")
  (exec-sql-statement "ALTER TABLE ONLY tasks RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tasks ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "UPDATE tasks SET id = CAST(id_v187 AS UUID) WHERE CHAR_LENGTH(id_v187) = 36")
  (exec-sql-statement "ALTER TABLE ONLY tasks ADD COLUMN tool_id UUID"))

;; cols to drop: hid, workspace_id, type, integration_data_id_v187
(defn- add-apps-table
  "Renames the existing transformation_activity table to apps and adds updated columns."
  []
  (println "\t* updating the transformation_activity table to apps")
  (exec-sql-statement "ALTER TABLE transformation_activity RENAME TO apps")
  (alter-id-column-to-uuid "apps")
  (exec-sql-statement "ALTER TABLE ONLY apps RENAME COLUMN integration_data_id TO integration_data_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY apps ADD COLUMN integration_data_id UUID"))

;; cols to drop: transformation_task_id, transformation_step_id
;; transformation_task_id -> app_id
;; transformation_step_id > transformation_steps.id
;; transformation_steps.transformation_id > transformations.id
;; transformations.template_id -> task_id
(defn- add-app-steps-table
  "Renames the existing transformation_task_steps table to app_steps and adds updated columns."
  []
  (println "\t* updating the transformation_task_steps table to app_steps")
  (exec-sql-statement "ALTER TABLE transformation_task_steps RENAME TO app_steps")
  (exec-sql-statement "ALTER TABLE ONLY app_steps ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY app_steps RENAME COLUMN hid TO step")
  (exec-sql-statement "ALTER TABLE ONLY app_steps ADD COLUMN app_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY app_steps ADD COLUMN task_id UUID"))

;; cols to drop: id_v187
(defn- alter-integration-data-table
  "Updates columns in the existing integration_data table."
  []
  (println "\t* updating the integration_data table")
  (exec-sql-statement "ALTER TABLE ONLY integration_data RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY integration_data ADD COLUMN id UUID DEFAULT (uuid_generate_v4())"))

;; cols to drop: id_v187, user_id_v187, transformation_activity_id, comment_id_v187
(defn- alter-ratings-table
  "Updates columns in the existing ratings table."
  []
  (println "\t* updating the ratings table")
  (exec-sql-statement "ALTER TABLE ONLY ratings RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY ratings ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY ratings RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY ratings ADD COLUMN user_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY ratings ADD COLUMN app_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY ratings RENAME COLUMN comment_id TO comment_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY ratings ADD COLUMN comment_id UUID"))

;; cols to drop: template_group_id, template_id
(defn- add-app-category-app-table
  "Renames the existing template_group_template table to app_category_app and adds updated columns."
  []
  (println "\t* updating the template_group_template table to app_category_app")
  (exec-sql-statement "ALTER TABLE template_group_template RENAME TO app_category_app")
  (exec-sql-statement "ALTER TABLE ONLY app_category_app ADD COLUMN app_category_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY app_category_app ADD COLUMN app_id UUID"))

;; cols to drop: id_v187
(defn- alter-data-formats-table
  "Updates columns in the existing data_formats table."
  []
  (println "\t* updating the data_formats table")
  (exec-sql-statement "ALTER TABLE ONLY data_formats RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY data_formats RENAME COLUMN guid TO id")
  (exec-sql-statement "ALTER TABLE ONLY data_formats ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

;; cols to drop: hid, source, target
(defn- add-workflow-io-maps-table
  "Renames the existing input_output_mapping table to workflow_io_maps and adds updated columns."
  []
  (println "\t* updating the input_output_mapping table to workflow_io_maps")
  (exec-sql-statement "ALTER TABLE input_output_mapping RENAME TO workflow_io_maps")
  (exec-sql-statement "ALTER TABLE ONLY workflow_io_maps ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY workflow_io_maps ADD COLUMN app_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY workflow_io_maps ADD COLUMN target_step UUID")
  (exec-sql-statement "ALTER TABLE ONLY workflow_io_maps ADD COLUMN source_step UUID"))

;; cols to drop: mapping_id_v187
(defn- readd-input-output-mapping-table
  "Renames the existing dataobject_mapping table to input_output_mapping and adds updated columns."
  []
  (println "\t* updating the dataobject_mapping table to input_output_mapping")
  (exec-sql-statement "ALTER TABLE dataobject_mapping RENAME TO input_output_mapping")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping RENAME COLUMN mapping_id TO mapping_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping ADD COLUMN mapping_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping RENAME COLUMN input TO input_v187")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping RENAME COLUMN output TO output_v187")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping ADD COLUMN input UUID")
  (exec-sql-statement "ALTER TABLE ONLY input_output_mapping ADD COLUMN output UUID"))

;; cols to drop: hid, info_type_v187, data_format_v187, multiplicity_v187, data_source_id_v187
;; rename orderd?
(defn- add-file-parameters-table
  "Renames the existing dataobjects table to file_parameters and adds updated columns."
  []
  (println "\t* updating the dataobjects table to file_parameters")
  (exec-sql-statement "ALTER TABLE dataobjects RENAME TO file_parameters")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters RENAME COLUMN info_type TO info_type_v187")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN info_type UUID")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters RENAME COLUMN data_format TO data_format_v187")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN data_format UUID")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters RENAME COLUMN multiplicity TO multiplicity_v187")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN multiplicity UUID")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters RENAME COLUMN data_source_id TO data_source_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN data_source_id  UUID"))

;; cols to drop: id_v187, deployed_component_id
(defn- add-tool-test-data-files-table
  "Renames the existing deployed_component_data_files table to tool_test_data_files and adds updated columns."
  []
  (println "\t* updating the deployed_component_data_files table to tool_test_data_files")
  (exec-sql-statement "ALTER TABLE deployed_component_data_files RENAME TO tool_test_data_files")
  (exec-sql-statement "ALTER TABLE ONLY tool_test_data_files RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_test_data_files ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY tool_test_data_files ADD COLUMN tool_id UUID"))

;; cols to drop: hid
(defn- alter-info-type-table
  "Updates columns in the existing info_type table."
  []
  (println "\t* updating the info_type table")
  (exec-sql-statement "ALTER TABLE ONLY info_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

;; cols to drop: hid
(defn- alter-multiplicity-table
  "Updates columns in the existing multiplicity table."
  []
  (println "\t* updating the multiplicity table")
  (exec-sql-statement "ALTER TABLE ONLY multiplicity ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

;; cols to drop: hid, property_type, validator, dataobject_id
(defn- add-parameters-table
  "Renames the existing property table to parameters and adds updated columns."
  []
  (println "\t* updating the property table to parameters")
  (exec-sql-statement "ALTER TABLE property RENAME TO parameters")
  (alter-id-column-to-uuid "parameters")
  (exec-sql-statement "ALTER TABLE ONLY parameters ADD COLUMN parameter_group_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY parameters ADD COLUMN parameter_type UUID")
  (exec-sql-statement "ALTER TABLE ONLY parameters ADD COLUMN required boolean DEFAULT false")
  (exec-sql-statement "ALTER TABLE ONLY parameters ADD COLUMN file_parameter_id UUID"))

;; cols to drop: hid, group_type?
(defn- add-parameter-groups-table
  "Renames the existing property_group table to parameter_groups and adds updated columns."
  []
  (println "\t* updating the property_group table to parameter_groups")
  (exec-sql-statement "ALTER TABLE property_group RENAME TO parameter_groups")
  (alter-id-column-to-uuid "parameter_groups")
  (exec-sql-statement "ALTER TABLE ONLY parameter_groups ADD COLUMN task_id UUID"))

(defn- add-parameter-values-table
  "Adds the new parameter_values table."
  []
  (println "\t* adding the parameter_values table")
  (load-sql-file "tables/25_parameter_values.sql"))

;; cols to drop: hid, value_type_id_v187
(defn- add-parameter-types-table
  "Renames the existing property_type table to parameter_types and adds updated columns."
  []
  (println "\t* updating the property_type table to parameter_types")
  (exec-sql-statement "ALTER TABLE property_type RENAME TO parameter_types")
  (exec-sql-statement "ALTER TABLE ONLY parameter_types ALTER COLUMN id TYPE UUID USING"
                      " CAST(regexp_replace(id, 'pt(.*)', '\\1') AS UUID)")
  (exec-sql-statement "ALTER TABLE ONLY parameter_types RENAME COLUMN value_type_id TO value_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY parameter_types ADD COLUMN value_type_id UUID"))

;; cols to drop: id_v187
(defn- alter-users-table
  "Updates columns in the existing users table."
  []
  (println "\t* updating the users table")
  (exec-sql-statement "ALTER TABLE ONLY users RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY users ADD COLUMN id UUID DEFAULT (uuid_generate_v4())"))

;; cols to drop: hid, name, description, label, rule_type_v187
(defn- add-validation-rules-table
  "Renames the existing rule table to validation_rules and adds updated columns."
  []
  (println "\t* updating the rule table to validation_rules")
  (exec-sql-statement "ALTER TABLE rule RENAME TO validation_rules")
  (alter-id-column-to-uuid "validation_rules")
  (exec-sql-statement "ALTER TABLE ONLY validation_rules ADD COLUMN parameter_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY validation_rules RENAME COLUMN rule_type TO rule_type_v187")
  (exec-sql-statement "ALTER TABLE ONLY validation_rules ADD COLUMN rule_type UUID"))

;; cols to drop: hid, rule_id_v187
(defn- add-validation-rule-arguments-table
  "Renames the existing rule_argument table to validation_rule_arguments and adds updated columns."
  []
  (println "\t* updating the rule_argument table to validation_rule_arguments")
  (exec-sql-statement "ALTER TABLE rule_argument RENAME TO validation_rule_arguments")
  (exec-sql-statement "ALTER TABLE ONLY validation_rule_arguments ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY validation_rule_arguments RENAME COLUMN rule_id TO rule_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY validation_rule_arguments ADD COLUMN rule_id UUID"))

;; cols to drop: hid
(defn- alter-rule-subtype-table
  "Updates columns in the existing rule_subtype table."
  []
  (println "\t* updating the rule_subtype table")
  (exec-sql-statement "ALTER TABLE ONLY rule_subtype ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

;; cols to drop: hid
(defn- alter-rule-type-table
  "Updates columns in the existing rule_type table."
  []
  (println "\t* updating the rule_type table")
  (exec-sql-statement "ALTER TABLE ONLY rule_type ALTER COLUMN id TYPE UUID USING"
                      " CAST(regexp_replace(id, 'rt(.*)', '\\1') AS UUID)")
  (exec-sql-statement "ALTER TABLE ONLY rule_type RENAME COLUMN rule_subtype_id TO rule_subtype_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY rule_type ADD COLUMN rule_subtype_id UUID"))

;; cols to drop: rule_type_id_v187, value_type_id_v187
(defn- alter-rule-type-value-type-table
  "Updates columns in the existing rule_type_value_type table."
  []
  (println "\t* updating the rule_type_value_type table")
  (exec-sql-statement "ALTER TABLE ONLY rule_type_value_type RENAME COLUMN rule_type_id TO rule_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY rule_type_value_type ADD COLUMN rule_type_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY rule_type_value_type RENAME COLUMN value_type_id TO value_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY rule_type_value_type ADD COLUMN value_type_id UUID"))

;; cols to drop: transformation_activity_id, template_group_id
(defn- alter-suggested_groups-table
  "Updates columns in the existing suggested_groups table."
  []
  (println "\t* updating the suggested_groups table")
  (exec-sql-statement "ALTER TABLE ONLY suggested_groups ADD COLUMN app_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY suggested_groups ADD COLUMN app_category_id UUID"))

;; cols to drop: hid, parent_group_id, subgroup_id
(defn- add-app-category-group-table
  "Renames the existing template_group_group table to app_category_group and adds updated columns."
  []
  (println "\t* updating the template_group_group table to app_category_group")
  (exec-sql-statement "ALTER TABLE template_group_group RENAME TO app_category_group")
  (exec-sql-statement "ALTER TABLE ONLY app_category_group ADD COLUMN parent_category_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY app_category_group ADD COLUMN child_category_id UUID"))

;; cols to drop: id_v187, transformation_activity_id
(defn- add-app-references-table
  "Renames the existing transformation_activity_references table to app_references and adds updated columns."
  []
  (println "\t* updating the transformation_activity_references table to app_references")
  (exec-sql-statement "ALTER TABLE transformation_activity_references RENAME TO app_references")
  (exec-sql-statement "ALTER TABLE ONLY app_references RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY app_references ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY app_references ADD COLUMN app_id UUID"))

;; cols to drop: hid
(defn- alter-value-type-table
  "Updates columns in the existing value_type table."
  []
  (println "\t* updating the value_type table")
  (exec-sql-statement "ALTER TABLE ONLY value_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

(defn- alter-version-table
  "Updates columns in the existing version table."
  []
  (println "\t* updating the version table")
  (exec-sql-statement "ALTER TABLE ONLY version DROP COLUMN id")
  (exec-sql-statement "ALTER TABLE ONLY version ADD PRIMARY KEY (version)"))

;; cols to drop: id_v187, created_by_v187, last_modified_by_v187
(defn- alter-genome-reference-table
  "Updates columns in the existing genome_reference table."
  []
  (println "\t* updating the genome_reference table")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference RENAME COLUMN uuid TO id")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference RENAME COLUMN created_by TO created_by_v187")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference ADD COLUMN created_by UUID")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference RENAME COLUMN last_modified_by TO last_modified_by_v187")
  (exec-sql-statement "ALTER TABLE ONLY genome_reference ADD COLUMN last_modified_by UUID"))

;; cols to drop: id_v187, user_id_v187, collaborator_id_v187
(defn- alter-collaborators-table
  "Updates columns in the existing collaborators table."
  []
  (println "\t* updating the collaborators table")
  (exec-sql-statement "ALTER TABLE ONLY collaborators RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY collaborators RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY collaborators ADD COLUMN user_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY collaborators RENAME COLUMN collaborator_id TO collaborator_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY collaborators ADD COLUMN collaborator_id UUID"))

;; cols to drop: id_v187
(defn- alter-data-source-table
  "Updates columns in the existing data_source table."
  []
  (println "\t* updating the data_source table")
  (exec-sql-statement "ALTER TABLE ONLY data_source RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY data_source RENAME COLUMN uuid TO id")
  (exec-sql-statement "ALTER TABLE ONLY data_source ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"))

;; cols to drop: id_v187
(defn- alter-tool-types-table
  "Updates columns in the existing tool_types table."
  []
  (println "\t* updating the tool_types table")
  (exec-sql-statement "ALTER TABLE ONLY tool_types RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_types ADD COLUMN id UUID DEFAULT (uuid_generate_v4())"))

;; cols to drop: tool_type_id_v187, property_type_id
(defn- add-tool-type-parameter-type-table
  "Renames the existing tool_type_property_type table to tool_type_parameter_type and adds updated columns."
  []
  (println "\t* updating the tool_type_property_type table to tool_type_parameter_type")
  (exec-sql-statement "ALTER TABLE tool_type_property_type RENAME TO tool_type_parameter_type")
  (exec-sql-statement "ALTER TABLE ONLY tool_type_parameter_type RENAME COLUMN tool_type_id TO tool_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_type_parameter_type ADD COLUMN tool_type_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY tool_type_parameter_type ADD COLUMN parameter_type_id UUID"))

;; cols to drop: id_v187
(defn- alter-tool-architectures-table
  "Updates columns in the existing tool_architectures table."
  []
  (println "\t* updating the tool_architectures table")
  (exec-sql-statement "ALTER TABLE ONLY tool_architectures RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_architectures ADD COLUMN id UUID DEFAULT (uuid_generate_v4())"))

;; cols to drop: id_v187, requestor_id_v187, tool_architecture_id_v187, deployed_component_id
(defn- alter-tool-requests-table
  "Updates columns in the existing tool_requests table."
  []
  (println "\t* updating the tool_requests table")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests RENAME COLUMN uuid TO id")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests RENAME COLUMN requestor_id TO requestor_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests ADD COLUMN requestor_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests RENAME COLUMN tool_architecture_id TO tool_architecture_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests ADD COLUMN tool_architecture_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY tool_requests ADD COLUMN tool_id UUID"))

;; cols to drop: id_v187, tool_request_id_v187, updater_id_v187
(defn- alter-tool-request-statuses-table
  "Updates columns in the existing tool_request_statuses table."
  []
  (println "\t* updating the tool_request_statuses table")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses ADD COLUMN id UUID DEFAULT (uuid_generate_v4())")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses RENAME COLUMN tool_request_id TO tool_request_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses ADD COLUMN tool_request_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses RENAME COLUMN updater_id TO updater_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY tool_request_statuses ADD COLUMN updater_id UUID"))

;; cols to drop: user_id_v187
(defn- alter-logins-table
  "Updates columns in the existing logins table."
  []
  (println "\t* updating the logins table")
  (exec-sql-statement "ALTER TABLE ONLY logins RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY logins ADD COLUMN user_id UUID"))

;; cols to drop: id_v187
(defn- alter-job_types-table
  "Updates columns in the existing job_types table."
  []
  (println "\t* updating the job_types table")
  (exec-sql-statement "ALTER TABLE ONLY job_types RENAME COLUMN id TO id_v187")
  (exec-sql-statement "ALTER TABLE ONLY job_types ADD COLUMN id UUID DEFAULT (uuid_generate_v4())"))

;; cols to drop: job_type_id_v187, user_id_v187
(defn- alter-jobs-table
  "Updates columns in the existing jobs table."
  []
  (println "\t* updating the jobs table")
  (exec-sql-statement "ALTER TABLE ONLY jobs RENAME COLUMN job_type_id TO job_type_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY jobs ADD COLUMN job_type_id UUID")
  (exec-sql-statement "ALTER TABLE ONLY jobs RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY jobs ADD COLUMN user_id UUID"))

(defn- alter-user-preferences-table
  "Updates columns in the existing user_preferences table."
  []
  (println "\t* updating the user_preferences table")
  (exec-sql-statement "ALTER TABLE ONLY user_preferences RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY user_preferences ADD COLUMN user_id UUID"))

(defn- alter-user-sessions-table
  "Updates columns in the existing user_sessions table."
  []
  (println "\t* updating the user_sessions table")
  (exec-sql-statement "ALTER TABLE ONLY user_sessions RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY user_sessions ADD COLUMN user_id UUID"))

(defn- alter-user-saved-searches-table
  "Updates columns in the existing user_saved_searches table."
  []
  (println "\t* updating the user_saved_searches table")
  (exec-sql-statement "ALTER TABLE ONLY user_saved_searches RENAME COLUMN user_id TO user_id_v187")
  (exec-sql-statement "ALTER TABLE ONLY user_saved_searches ADD COLUMN user_id UUID"))


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
  (add-app-categories-table)
  (alter-workspace-table)
  (add-tools-table)
  (add-tasks-table)
  (add-apps-table)
  (add-app-steps-table)
  (alter-integration-data-table)
  (alter-ratings-table)
  (add-app-category-app-table)
  (alter-data-formats-table)
  (add-workflow-io-maps-table)
  (readd-input-output-mapping-table)
  (add-file-parameters-table)
  (add-tool-test-data-files-table)
  (alter-info-type-table)
  (alter-multiplicity-table)
  (add-parameters-table)
  (add-parameter-groups-table)
  (add-parameter-values-table)
  (add-parameter-types-table)
  (alter-users-table)
  (add-validation-rules-table)
  (add-validation-rule-arguments-table)
  (alter-rule-subtype-table)
  (alter-rule-type-table)
  (alter-rule-type-value-type-table)
  (alter-suggested_groups-table)
  (add-app-category-group-table)
  (add-app-references-table)
  (alter-value-type-table)
  (alter-version-table)
  (alter-genome-reference-table)
  (alter-collaborators-table)
  (alter-data-source-table)
  (alter-tool-types-table)
  (add-tool-type-parameter-type-table)
  (alter-tool-architectures-table)
  (alter-tool-requests-table)
  (alter-tool-request-statuses-table)
  (alter-logins-table)
  (alter-job_types-table)
  (alter-jobs-table)
  (alter-user-preferences-table)
  (alter-user-sessions-table)
  (alter-user-saved-searches-table)
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
