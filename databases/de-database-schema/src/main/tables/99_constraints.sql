--
-- Name: data_formats_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY data_formats
    ADD CONSTRAINT data_formats_pkey
    PRIMARY KEY (id);

--
-- Name: workflow_io_maps_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY workflow_io_maps
    ADD CONSTRAINT workflow_io_maps_pkey
    PRIMARY KEY (id);
CREATE INDEX workflow_io_maps_app_id_idx ON workflow_io_maps(app_id);
CREATE INDEX workflow_io_maps_source_idx ON workflow_io_maps(source_step);
CREATE INDEX workflow_io_maps_target_idx ON workflow_io_maps(target_step);

--
-- Name: file_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY file_parameters
    ADD CONSTRAINT file_parameters_pkey
    PRIMARY KEY (id);

--
-- Name: tool_test_data_files_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY tool_test_data_files
    ADD CONSTRAINT tool_test_data_files_pkey
    PRIMARY KEY (id);

--
-- Name: tools_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY tools
    ADD CONSTRAINT tools_pkey
    PRIMARY KEY (id);

--
-- Name: info_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY info_type
    ADD CONSTRAINT info_type_pkey
    PRIMARY KEY (id);

--
-- Name: input_output_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_pkey
    PRIMARY KEY (mapping_id, input);

--
-- Name: integration_data_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY integration_data
    ADD CONSTRAINT integration_data_pkey
    PRIMARY KEY (id);

--
-- Name: parameter_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY parameter_groups
    ADD CONSTRAINT parameter_groups_pkey
    PRIMARY KEY (id);

--
-- Name: parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY parameters
    ADD CONSTRAINT parameters_pkey
    PRIMARY KEY (id);

--
-- Name: parameter_values_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY parameter_values
    ADD CONSTRAINT parameter_values_pkey
    PRIMARY KEY (id);

--
-- Name: parameter_types_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY parameter_types
    ADD CONSTRAINT parameter_types_pkey
    PRIMARY KEY (id);

--
-- Name: ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_pkey
    PRIMARY KEY (id);

--
-- Name: validation_rule_arguments_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY validation_rule_arguments
    ADD CONSTRAINT validation_rule_arguments_pkey
    PRIMARY KEY (id);
CREATE INDEX validation_rule_arguments_rule_id_idx ON validation_rule_arguments(rule_id);

--
-- Name: validation_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: de; Tablespace:
--
ALTER TABLE ONLY validation_rules
    ADD CONSTRAINT validation_rules_pkey
    PRIMARY KEY (id);

--
-- Name: rule_subtype_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY rule_subtype
    ADD CONSTRAINT rule_subtype_pkey
    PRIMARY KEY (id);

--
-- Name: rule_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY rule_type
    ADD CONSTRAINT rule_type_pkey
    PRIMARY KEY (id);

--
-- Name: suggested_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_pkey
    PRIMARY KEY (app_id, app_category_id);

--
-- Name: app_category_group_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY app_category_group
    ADD CONSTRAINT app_category_group_pkey
    PRIMARY KEY (parent_category_id, child_category_id);

--
-- Name: app_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY app_categories
    ADD CONSTRAINT app_categories_pkey
    PRIMARY KEY (id);

--
-- Name: app_category_app_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY app_category_app
    ADD CONSTRAINT app_category_app_pkey
    PRIMARY KEY (app_category_id, app_id);

--
-- Name: tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY tasks
    ADD CONSTRAINT tasks_pkey
    PRIMARY KEY (id);

--
-- Name: apps_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY apps
    ADD CONSTRAINT apps_pkey
    PRIMARY KEY (id);

--
-- Name: app_references_pkey; Type: CONSTRAINT; Schema:
-- public; Owner: de; Tablespace:
--
ALTER TABLE ONLY app_references
    ADD CONSTRAINT app_references_pkey
    PRIMARY KEY (id);

--
-- Name: app_steps_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY app_steps
    ADD CONSTRAINT app_steps_pkey
    PRIMARY KEY (id);

--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: de; Tablespace:
--
ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey
    PRIMARY KEY (id);

--
-- Name: username_unique; Type: CONSTRAINT; Schema: public; Owner de;
-- Tablespace:
--
ALTER TABLE ONLY users
    ADD CONSTRAINT username_unique
    UNIQUE (username);

--
-- Name: value_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY value_type
    ADD CONSTRAINT value_type_pkey
    PRIMARY KEY (id);

--
-- Name: votes_unique; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT votes_unique
    UNIQUE (user_id, app_id);

--
-- Name: workspace_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY workspace
    ADD CONSTRAINT workspace_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the version table.
--
ALTER TABLE ONLY version
    ADD CONSTRAINT version_pkey
    PRIMARY KEY (version);

--
-- Primary Key for the tool_types table.
--
ALTER TABLE ONLY tool_types
    ADD CONSTRAINT tool_types_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the tool_request_status_codes table.
--
ALTER TABLE ONLY tool_request_status_codes
    ADD CONSTRAINT tool_request_status_codes_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the tool_architectures table.
--
ALTER TABLE ONLY tool_architectures
    ADD CONSTRAINT tool_architectures_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the tool_request_statuses table.
--
ALTER TABLE ONLY tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the job_types table.
--
ALTER TABLE ONLY job_types
    ADD CONSTRAINT job_types_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the metadata_value_types table.
--
ALTER TABLE ONLY metadata_value_types
    ADD CONSTRAINT metadata_value_types_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the metadata_templates table.
--
ALTER TABLE ONLY metadata_templates
    ADD CONSTRAINT metadata_templates_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
    ADD CONSTRAINT metadata_attributes_pkey
    PRIMARY KEY (id);

--
-- Primary Key for the tree_urls table.
--
ALTER TABLE ONLY tree_urls
    ADD CONSTRAINT tree_urls_pkey
    PRIMARY KEY (id);

--
-- Name: input_output_mapping_mapping_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_mapping_id_fk
    FOREIGN KEY (mapping_id)
    REFERENCES workflow_io_maps(id) ON DELETE CASCADE;

--
-- Name: input_output_mapping_input_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_input_fkey
    FOREIGN KEY (input)
    REFERENCES parameters(id) ON DELETE CASCADE;

--
-- Name: input_output_mapping_output_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_output_fkey
    FOREIGN KEY (output)
    REFERENCES parameters(id) ON DELETE CASCADE;

--
-- Name: app_categories_workspace_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY app_categories
    ADD CONSTRAINT app_categories_workspace_id_fk
    FOREIGN KEY (workspace_id)
    REFERENCES workspace(id);

--
-- Name: file_parameters_data_format_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY file_parameters
    ADD CONSTRAINT file_parameters_data_format_fkey
    FOREIGN KEY (data_format)
    REFERENCES data_formats(id);

--
-- Name: file_parameters_info_type_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY file_parameters
    ADD CONSTRAINT file_parameters_info_type_fkey
    FOREIGN KEY (info_type)
    REFERENCES info_type(id);

--
-- Name: deployed_comp_integration_data_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY tools
    ADD CONSTRAINT deployed_comp_integration_data_id_fk
    FOREIGN KEY (integration_data_id)
    REFERENCES integration_data(id);

--
-- Name: tool_test_data_files_tool_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY tool_test_data_files
    ADD CONSTRAINT tool_test_data_files_tool_id_fkey
    FOREIGN KEY (tool_id)
    REFERENCES tools(id);

--
-- Name: workflow_io_maps_unique; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY workflow_io_maps
    ADD CONSTRAINT workflow_io_maps_unique
    UNIQUE (app_id, target_step, source_step);

--
-- Name: workflow_io_maps_app_id_fkey;
-- Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY workflow_io_maps
    ADD CONSTRAINT workflow_io_maps_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: workflow_io_maps_source_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY workflow_io_maps
    ADD CONSTRAINT workflow_io_maps_source_fkey
    FOREIGN KEY (source_step)
    REFERENCES app_steps(id) ON DELETE CASCADE;

--
-- Name: workflow_io_maps_target_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY workflow_io_maps
    ADD CONSTRAINT workflow_io_maps_target_fkey
    FOREIGN KEY (target_step)
    REFERENCES app_steps(id) ON DELETE CASCADE;

--
-- Name: file_parameters_parameter_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY file_parameters
    ADD CONSTRAINT file_parameters_parameter_id_fkey
    FOREIGN KEY (parameter_id)
    REFERENCES parameters(id) ON DELETE CASCADE;

--
-- Name: parameters_parameter_groups_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY parameters
    ADD CONSTRAINT parameters_parameter_groups_id_fkey
    FOREIGN KEY (parameter_group_id)
    REFERENCES parameter_groups(id) ON DELETE CASCADE;

--
-- Name: parameters_parameter_types_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY parameters
    ADD CONSTRAINT parameters_parameter_types_fkey
    FOREIGN KEY (parameter_type)
    REFERENCES parameter_types(id);

--
-- Name: parameter_values_parameter_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY parameter_values
    ADD CONSTRAINT parameter_values_parameter_id_fkey
    FOREIGN KEY (parameter_id)
    REFERENCES parameters(id) ON DELETE CASCADE;
CREATE INDEX parameter_values_parameter_id_idx ON parameter_values(parameter_id);

--
-- Name: parameter_values_parent_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY parameter_values
    ADD CONSTRAINT parameter_values_parent_id_fkey
    FOREIGN KEY (parent_id)
    REFERENCES parameter_values(id) ON DELETE CASCADE;
CREATE INDEX parameter_values_parent_id_idx ON parameter_values(parent_id);

--
-- Name: parameter_types_value_type_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY parameter_types
    ADD CONSTRAINT parameter_types_value_type_fkey
    FOREIGN KEY (value_type_id)
    REFERENCES value_type(id);

--
-- Name: ratings_app_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: ratings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Name: validation_rule_arguments_validation_rules_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY validation_rule_arguments
    ADD CONSTRAINT validation_rule_arguments_validation_rules_id_fkey
    FOREIGN KEY (rule_id)
    REFERENCES validation_rules(id) ON DELETE CASCADE;

--
-- Name: validation_rules_rule_type_fkey; Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY validation_rules
    ADD CONSTRAINT validation_rules_rule_type_fkey
    FOREIGN KEY (rule_type)
    REFERENCES rule_type(id);

--
-- Name: rule_type_rule_subtype_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY rule_type
    ADD CONSTRAINT rule_type_rule_subtype_id_fkey
    FOREIGN KEY (rule_subtype_id)
    REFERENCES rule_subtype(id);

--
-- Name: rule_type_value_type_rule_type_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY rule_type_value_type
    ADD CONSTRAINT rule_type_value_type_rule_type_id_fkey
    FOREIGN KEY (rule_type_id)
    REFERENCES rule_type(id);

--
-- Name: rule_type_value_type_value_type_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY rule_type_value_type
    ADD CONSTRAINT rule_type_value_type_value_type_id_fkey
    FOREIGN KEY (value_type_id)
    REFERENCES value_type(id);

--
-- Name: suggested_groups_app_category_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_app_category_id_fkey
    FOREIGN KEY (app_category_id)
    REFERENCES app_categories(id) ON DELETE CASCADE;

--
-- Name: suggested_groups_app_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: app_category_group_parent_category_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY app_category_group
    ADD CONSTRAINT app_category_group_parent_category_id_fkey
    FOREIGN KEY (parent_category_id)
    REFERENCES app_categories(id) ON DELETE CASCADE;

--
-- Name: app_category_group_child_category_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY app_category_group
    ADD CONSTRAINT app_category_group_child_category_id_fkey
    FOREIGN KEY (child_category_id)
    REFERENCES app_categories(id) ON DELETE CASCADE;

--
-- Name: app_category_app_app_category_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY app_category_app
    ADD CONSTRAINT app_category_app_app_category_id_fkey
    FOREIGN KEY (app_category_id)
    REFERENCES app_categories(id) ON DELETE CASCADE;

--
-- Name: app_category_app_app_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY app_category_app
    ADD CONSTRAINT app_category_app_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: parameter_groups_task_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY parameter_groups
    ADD CONSTRAINT parameter_groups_task_id_fkey
    FOREIGN KEY (task_id)
    REFERENCES tasks(id) ON DELETE CASCADE;

--
-- Name: app_integration_data_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY apps
    ADD CONSTRAINT app_integration_data_id_fk
    FOREIGN KEY (integration_data_id)
    REFERENCES integration_data(id);

--
-- Name: app_references_app_id_fkey;
-- Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY app_references
    ADD CONSTRAINT app_references_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: app_steps_task_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY app_steps
    ADD CONSTRAINT app_steps_task_id_fkey
    FOREIGN KEY (task_id)
    REFERENCES tasks(id) ON DELETE CASCADE;

--
-- Name: app_steps_app_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY app_steps
    ADD CONSTRAINT app_steps_app_id_fkey
    FOREIGN KEY (app_id)
    REFERENCES apps(id) ON DELETE CASCADE;

--
-- Name: validation_rules_parameters_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY validation_rules
    ADD CONSTRAINT validation_rules_parameters_id_fkey
    FOREIGN KEY (parameter_id)
    REFERENCES parameters(id) ON DELETE CASCADE;
CREATE INDEX validation_rules_parameters_id_idx ON validation_rules(parameter_id);

--
-- Name: workspace_root_category_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY workspace
    ADD CONSTRAINT workspace_root_category_id_fkey
    FOREIGN KEY (root_category_id)
    REFERENCES app_categories(id);

--
-- Name: workspace_users_fk; Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY workspace
    ADD CONSTRAINT workspace_users_fk
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Name: genome_reference_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
--
ALTER TABLE ONLY genome_reference
    ADD CONSTRAINT genome_reference_pkey
    PRIMARY KEY (id);

--
-- Name: genome_reference_created_by_fkey; Type: CONSTRAINT; Schema:
-- public; Owner: de;
--
ALTER TABLE ONLY genome_reference
    ADD CONSTRAINT genome_reference_created_by_fkey
    FOREIGN KEY (created_by)
    REFERENCES users(id);

--
-- Name: genome_reference_last_modified_by_fkey; Type: CONSTRAINT; Schema:
-- public; Owner: de;
--
ALTER TABLE ONLY genome_reference
    ADD CONSTRAINT genome_reference_last_modified_by_fkey
    FOREIGN KEY (last_modified_by)
    REFERENCES users(id);

--
-- The primary key for the collaborators table.
--
ALTER TABLE ONLY collaborators
    ADD CONSTRAINT collaborators_pkey
    PRIMARY KEY (user_id, collaborator_id);

--
-- Foreign key constraints for the user field of the collaborators table.
--
ALTER TABLE ONLY collaborators
    ADD CONSTRAINT collaborators_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);
--
-- Foreign key constraints for the collaborator_id field of the collaborators
-- table.
--
ALTER TABLE ONLY collaborators
    ADD CONSTRAINT collaborators_collaborator_id_fkey
    FOREIGN KEY (collaborator_id)
    REFERENCES users(id);

--
-- Add a uniqueness constraint for the integration_data table.
--
ALTER TABLE ONLY integration_data
    ADD CONSTRAINT integration_data_name_email_unique
    UNIQUE (integrator_name, integrator_email);

--
-- The primary key for the data_source table.
--
ALTER TABLE ONLY data_source
    ADD CONSTRAINT data_source_pkey
    PRIMARY KEY (id);

--
-- Each data source must have a unique name.
--
ALTER TABLE ONLY data_source
    ADD CONSTRAINT data_source_unique_name
    UNIQUE (name);

--
-- Foreign key constraint for the data_source field of the file_parameters table.
--
ALTER TABLE ONLY file_parameters
    ADD CONSTRAINT file_parameters_data_source_id_fkey
    FOREIGN KEY (data_source_id)
    REFERENCES data_source(id);

--
-- Name: tasks_tool_id_fk; Type: CONSTRAINT; Schema: public; Owner: de;
--
ALTER TABLE ONLY tasks
    ADD CONSTRAINT tasks_tool_id_fk
    FOREIGN KEY (tool_id)
    REFERENCES tools(id);

--
-- Foreign key constraint for the tool_type_id field of the tools
-- table.
--
ALTER TABLE ONLY tools
    ADD CONSTRAINT tools_tool_type_id_fkey
    FOREIGN KEY (tool_type_id)
    REFERENCES tool_types(id);

--
-- Foreign key constraint for the tool_type_id field of the
-- tool_type_parameter_type table.
--
ALTER TABLE ONLY tool_type_parameter_type
    ADD CONSTRAINT tool_type_parameter_type_tool_type_id_fkey
    FOREIGN KEY (tool_type_id)
    REFERENCES tool_types(id);

--
-- Foreign key constraint for the parameter_type_id field of the
-- tool_type_parameter_type table.
--
ALTER TABLE ONLY tool_type_parameter_type
    ADD CONSTRAINT tool_type_parameter_type_parameter_types_fkey
    FOREIGN KEY (parameter_type_id)
    REFERENCES parameter_types(id);

--
-- Foreign key constraint for the requestor_id field of the
-- tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_requestor_id_fkey
    FOREIGN KEY (requestor_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the tool_architecture_id field of the
-- tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_tool_architecture_id_fkey
    FOREIGN KEY (tool_architecture_id)
    REFERENCES tool_architectures(id);

--
-- Foreign key constraint for the tool_id field of the
-- tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_tool_id_fkey
    FOREIGN KEY (tool_id)
    REFERENCES tools(id);

--
-- Foreign key constraint for the updater_id field of the tool_request_statuses
-- table.
--
ALTER TABLE ONLY tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_updater_id_fkey
    FOREIGN KEY (updater_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the tool_request_id field of the tool_request_statuses
-- table.
--
ALTER TABLE ONLY tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_tool_request_id_fkey
    FOREIGN KEY (tool_request_id)
    REFERENCES tool_requests(id);

--
-- Foreign key constraint for the tool_request_status_code_id field of the tool_request_statuses
-- table.
--
ALTER TABLE ONLY tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_tool_request_status_code_id_fkey
    FOREIGN KEY (tool_request_status_code_id)
    REFERENCES tool_request_status_codes(id);

--
-- Foreign key constraint for the user_id field of the logins table.
--
ALTER TABLE ONLY logins
    ADD CONSTRAINT logins_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the value_type_id field of the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
    ADD CONSTRAINT metadata_attributes_value_type_id_fkey
    FOREIGN KEY (value_type_id)
    REFERENCES metadata_value_types(id);

--
-- Foreign key constraint for the template_id field of the metadata_template_attrs table.
--
ALTER TABLE ONLY metadata_template_attrs
    ADD CONSTRAINT metadata_template_attrs_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES metadata_templates(id);

--
-- Foreign key constraint for the attribute_id field of the metadata_template_attrs table.
--
ALTER TABLE ONLY metadata_template_attrs
    ADD CONSTRAINT metadata_template_attrs_attribute_id_fkey
    FOREIGN KEY (attribute_id)
    REFERENCES metadata_attributes(id);

--
-- Foreign key constraint for the attribute_id field of the metadata_attr_synonyms table.
--
ALTER TABLE ONLY metadata_attr_synonyms
    ADD CONSTRAINT metadata_attr_synonyms_attribute_id_fkey
    FOREIGN KEY (attribute_id)
    REFERENCES metadata_attributes(id);

--
-- Foreign key constraint for the synonym_id field of the metadata_attr_synonyms table.
--
ALTER TABLE ONLY metadata_attr_synonyms
    ADD CONSTRAINT metadata_attr_synonyms_synonym_id_fkey
    FOREIGN KEY (synonym_id)
    REFERENCES metadata_attributes(id);

--
-- The primary key for the user_preferences table.
--
ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT user_preferences_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the user_id field of the user_preferences table.
--
ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT user_preferences_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);
CREATE INDEX user_preferences_user_id_idx ON user_preferences(user_id);

--
-- The primary key for the user_sessions table.
--
ALTER TABLE ONLY user_sessions
    ADD CONSTRAINT user_sessions_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the user_id field of the user_sessions table.
--
ALTER TABLE ONLY user_sessions
    ADD CONSTRAINT user_sessions_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);
CREATE INDEX user_sessions_user_id_idx ON user_sessions(user_id);

--
-- The primary key for the user_saved_searches table.
--
ALTER TABLE ONLY user_saved_searches
    ADD CONSTRAINT user_saved_searches_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the user_id field of the user_saved_searches table.
--
ALTER TABLE ONLY user_saved_searches
    ADD CONSTRAINT user_saved_searches_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);
CREATE INDEX user_saved_searches_user_id_idx ON user_sessions(user_id);

--
-- The primary key for the access_tokens table.
--
ALTER TABLE ONLY access_tokens
    ADD CONSTRAINT access_tokens_pkey
    PRIMARY KEY (webapp, user_id);

--
-- Foreign key constraint for the user_id column of the access_tokens
-- table.
--
ALTER TABLE ONLY access_tokens
    ADD CONSTRAINT access_tokens_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- The primary key for the authorization_requests table.
--
ALTER TABLE ONLY authorization_requests
    ADD CONSTRAINT authorization_requests_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the user_id column of the authorization_requests
-- table.
--
ALTER TABLE ONLY authorization_requests
    ADD CONSTRAINT authorization_requests_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- The primary key for the jobs table.
--
ALTER TABLE ONLY jobs
    ADD CONSTRAINT jobs_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the user_id field of the jobs table.
--
ALTER TABLE ONLY jobs
    ADD CONSTRAINT jobs_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- The primary key for the job_steps table.
--
ALTER TABLE ONLY job_steps
    ADD CONSTRAINT job_steps_pkey
    PRIMARY KEY (job_id, step_number);

--
-- Foreign key constraint for the job_id field of the job_steps table.
--
ALTER TABLE ONLY job_steps
    ADD CONSTRAINT job_steps_job_id_fkey
    FOREIGN KEY (job_id)
    REFERENCES jobs(id);

--
-- Foreign key constraint for the type_type_id field of the job_steps table.
--
ALTER TABLE ONLY job_steps
    ADD CONSTRAINT job_steps_job_type_id_fkey
    FOREIGN KEY (job_type_id)
    REFERENCES job_types(id);
