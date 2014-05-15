--
-- Name: data_format_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY data_formats
    ADD CONSTRAINT data_format_pkey
    PRIMARY KEY (id);

--
-- Name: dataobject_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY dataobject_mapping
    ADD CONSTRAINT dataobject_mapping_pkey
    PRIMARY KEY (mapping_id, output);

--
-- Name: dataobjects_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_pkey
    PRIMARY KEY (hid);

--
-- Name: deployed_component_data_files_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY deployed_component_data_files
    ADD CONSTRAINT deployed_component_data_files_pkey
    PRIMARY KEY (id);

--
-- Name: deployed_components_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY deployed_components
    ADD CONSTRAINT deployed_components_pkey
    PRIMARY KEY (hid);

--
-- Name: info_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY info_type
    ADD CONSTRAINT info_type_pkey
    PRIMARY KEY (hid);

--
-- Name: input_output_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_pkey
    PRIMARY KEY (hid);

--
-- Name: integration_data_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY integration_data
    ADD CONSTRAINT integration_data_pkey
    PRIMARY KEY (id);

--
-- Name: multiplicity_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY multiplicity
    ADD CONSTRAINT multiplicity_pkey
    PRIMARY KEY (hid);

--
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey
    PRIMARY KEY (hid);

--
-- Name: notification_set_notification_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY notification_set_notification
    ADD CONSTRAINT notification_set_notification_pkey
    PRIMARY KEY (notification_set_id, hid);

--
-- Name: notification_set_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY notification_set
    ADD CONSTRAINT notification_set_pkey
    PRIMARY KEY (hid);

--
-- Name: notifications_receivers_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY notifications_receivers
    ADD CONSTRAINT notifications_receivers_pkey
    PRIMARY KEY (notification_id, hid);

--
-- Name: property_group_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY property_group
    ADD CONSTRAINT property_group_pkey
    PRIMARY KEY (hid);

--
-- Name: property_group_property_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY property_group_property
    ADD CONSTRAINT property_group_property_pkey
    PRIMARY KEY (property_group_id, hid);

--
-- Name: property_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY property
    ADD CONSTRAINT property_pkey
    PRIMARY KEY (hid);

--
-- Name: property_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY property_type
    ADD CONSTRAINT property_type_pkey
    PRIMARY KEY (hid);

--
-- Name: ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_pkey
    PRIMARY KEY (id);

--
-- Name: rule_argument_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY rule_argument
    ADD CONSTRAINT rule_argument_pkey
    PRIMARY KEY (rule_id, hid);

--
-- Name: rule_pkey; Type: CONSTRAINT; Schema: public; Owner: de; Tablespace:
--
ALTER TABLE ONLY rule
    ADD CONSTRAINT rule_pkey
    PRIMARY KEY (hid);

--
-- Name: rule_subtype_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY rule_subtype
    ADD CONSTRAINT rule_subtype_pkey
    PRIMARY KEY (hid);

--
-- Name: rule_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY rule_type
    ADD CONSTRAINT rule_type_pkey
    PRIMARY KEY (hid);

--
-- Name: suggested_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_pkey
    PRIMARY KEY (transformation_activity_id, template_group_id);

--
-- Name: template_group_group_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY template_group_group
    ADD CONSTRAINT template_group_group_pkey
    PRIMARY KEY (parent_group_id, hid);

--
-- Name: template_group_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY template_group
    ADD CONSTRAINT template_group_pkey
    PRIMARY KEY (hid);

--
-- Name: template_group_template_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY template_group_template
    ADD CONSTRAINT template_group_template_pkey
    PRIMARY KEY (template_group_id, template_id);

--
-- Name: template_input_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY template_input
    ADD CONSTRAINT template_input_pkey
    PRIMARY KEY (template_id, hid);

--
-- Name: template_output_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY template_output
    ADD CONSTRAINT template_output_pkey
    PRIMARY KEY (template_id, hid);

--
-- Name: template_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY template
    ADD CONSTRAINT template_pkey
    PRIMARY KEY (hid);

--
-- Name: template_property_group_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY template_property_group
    ADD CONSTRAINT template_property_group_pkey
    PRIMARY KEY (template_id, hid);

--
-- Name: transformation_activity_mappings_pkey; Type: CONSTRAINT; Schema:
-- public; Owner: de; Tablespace:
--
ALTER TABLE ONLY transformation_activity_mappings
    ADD CONSTRAINT transformation_activity_mappings_pkey
    PRIMARY KEY (transformation_activity_id, hid);

--
-- Name: transformation_activity_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY transformation_activity
    ADD CONSTRAINT transformation_activity_pkey
    PRIMARY KEY (hid);

--
-- Name: transformation_activity_references_pkey; Type: CONSTRAINT; Schema:
-- public; Owner: de; Tablespace:
--
ALTER TABLE ONLY transformation_activity_references
    ADD CONSTRAINT transformation_activity_references_pkey
    PRIMARY KEY (id);

--
-- Name: transformation_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY transformations
    ADD CONSTRAINT transformation_pkey
    PRIMARY KEY (id);

--
-- Name: transformation_step_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY transformation_steps
    ADD CONSTRAINT transformation_step_pkey
    PRIMARY KEY (id);

--
-- Name: transformation_task_steps_pkey; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY transformation_task_steps
    ADD CONSTRAINT transformation_task_steps_pkey
    PRIMARY KEY (transformation_task_id, hid);

--
-- Name: transformation_values_pkey; Type: CONSTRAINT; Schema: public; Owner:
-- de; Tablespace:
--
ALTER TABLE ONLY transformation_values
    ADD CONSTRAINT transformation_values_pkey
    PRIMARY KEY (id);

--
-- Name: transformation_values_unique; Type: CONSTRAINT; Schema: public;
-- Owner: de; Tablespace:
--
ALTER TABLE ONLY transformation_values
    ADD CONSTRAINT transformation_values_unique
    UNIQUE (transformation_id, property);

--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: de; Tablespace:
--
ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey
    PRIMARY KEY (id);

--
-- Name: validator_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY validator
    ADD CONSTRAINT validator_pkey
    PRIMARY KEY (hid);

--
-- Name: validator_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY validator_rule
    ADD CONSTRAINT validator_rule_pkey
    PRIMARY KEY (validator_id, id);

--
-- Name: value_type_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY value_type
    ADD CONSTRAINT value_type_pkey
    PRIMARY KEY (hid);

--
-- Name: votes_unique; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT votes_unique
    UNIQUE (user_id, transformation_activity_id);

--
-- Name: workspace_pkey; Type: CONSTRAINT; Schema: public; Owner: de;
-- Tablespace:
--
ALTER TABLE ONLY workspace
    ADD CONSTRAINT workspace_pkey
    PRIMARY KEY (id);

--
-- Name: dataobject_mapping_mapping_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY dataobject_mapping
    ADD CONSTRAINT dataobject_mapping_mapping_id_fkey
    FOREIGN KEY (mapping_id)
    REFERENCES input_output_mapping(hid);

--
-- Name: dataobjects_data_format_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_data_format_fkey
    FOREIGN KEY (data_format)
    REFERENCES data_formats(id);

--
-- Name: dataobjects_info_type_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_info_type_fkey
    FOREIGN KEY (info_type)
    REFERENCES info_type(hid);

--
-- Name: dataobjects_multiplicity_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_multiplicity_fkey
    FOREIGN KEY (multiplicity)
    REFERENCES multiplicity(hid);

--
-- Name: deployed_comp_integration_data_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY deployed_components
    ADD CONSTRAINT deployed_comp_integration_data_id_fk
    FOREIGN KEY (integration_data_id)
    REFERENCES integration_data(id);

--
-- Name: deployed_component_data_files_deployed_component_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY deployed_component_data_files
    ADD CONSTRAINT deployed_component_data_files_deployed_component_id_fkey
    FOREIGN KEY (deployed_component_id)
    REFERENCES deployed_components(hid);

--
-- Name: input_output_mapping_source_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_source_fkey
    FOREIGN KEY (source)
    REFERENCES transformation_steps(id);

--
-- Name: input_output_mapping_target_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY input_output_mapping
    ADD CONSTRAINT input_output_mapping_target_fkey
    FOREIGN KEY (target)
    REFERENCES transformation_steps(id);

--
-- Name: notification_set_notification_notification_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY notification_set_notification
    ADD CONSTRAINT notification_set_notification_notification_id_fkey
    FOREIGN KEY (notification_id)
    REFERENCES notification(hid);

--
-- Name: notification_set_notification_notification_set_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY notification_set_notification
    ADD CONSTRAINT notification_set_notification_notification_set_id_fkey
    FOREIGN KEY (notification_set_id)
    REFERENCES notification_set(hid);

--
-- Name: notifications_receivers_notification_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY notifications_receivers
    ADD CONSTRAINT notifications_receivers_notification_id_fkey
    FOREIGN KEY (notification_id)
    REFERENCES notification(hid);

--
-- Name: property_dataobject_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY property
    ADD CONSTRAINT property_dataobject_id_fkey
    FOREIGN KEY (dataobject_id)
    REFERENCES dataobjects(hid);

--
-- Name: property_group_property_property_group_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY property_group_property
    ADD CONSTRAINT property_group_property_property_group_id_fkey
    FOREIGN KEY (property_group_id)
    REFERENCES property_group(hid);

--
-- Name: property_group_property_property_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY property_group_property
    ADD CONSTRAINT property_group_property_property_id_fkey
    FOREIGN KEY (property_id)
    REFERENCES property(hid);

--
-- Name: property_property_type_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY property
    ADD CONSTRAINT property_property_type_fkey
    FOREIGN KEY (property_type)
    REFERENCES property_type(hid);

--
-- Name: property_validator_fkey; Type: FK CONSTRAINT; Schema: public; Owner:
-- de
--
ALTER TABLE ONLY property
    ADD CONSTRAINT property_validator_fkey
    FOREIGN KEY (validator)
    REFERENCES validator(hid);

--
-- Name: ratings_transformation_activity_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_transformation_activity_id_fkey
    FOREIGN KEY (transformation_activity_id)
    REFERENCES transformation_activity(hid);

--
-- Name: ratings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY ratings
    ADD CONSTRAINT ratings_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Name: rule_argument_rule_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY rule_argument
    ADD CONSTRAINT rule_argument_rule_id_fkey
    FOREIGN KEY (rule_id)
    REFERENCES rule(hid);

--
-- Name: rule_rule_type_fkey; Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY rule
    ADD CONSTRAINT rule_rule_type_fkey
    FOREIGN KEY (rule_type)
    REFERENCES rule_type(hid);

--
-- Name: rule_type_value_type_rule_type_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY rule_type_value_type
    ADD CONSTRAINT rule_type_value_type_rule_type_id_fkey
    FOREIGN KEY (rule_type_id)
    REFERENCES rule_type(hid);

--
-- Name: rule_type_value_type_value_type_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY rule_type_value_type
    ADD CONSTRAINT rule_type_value_type_value_type_id_fkey
    FOREIGN KEY (value_type_id)
    REFERENCES value_type(hid);

--
-- Name: suggested_groups_template_group_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_template_group_id_fkey
    FOREIGN KEY (template_group_id)
    REFERENCES template_group(hid);

--
-- Name: suggested_groups_transformation_activity_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY suggested_groups
    ADD CONSTRAINT suggested_groups_transformation_activity_id_fkey
    FOREIGN KEY (transformation_activity_id)
    REFERENCES transformation_activity(hid);

--
-- Name: template_group_group_parent_group_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY template_group_group
    ADD CONSTRAINT template_group_group_parent_group_id_fkey
    FOREIGN KEY (parent_group_id)
    REFERENCES template_group(hid);

--
-- Name: template_group_group_subgroup_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY template_group_group
    ADD CONSTRAINT template_group_group_subgroup_id_fkey
    FOREIGN KEY (subgroup_id)
    REFERENCES template_group(hid);

--
-- Name: template_group_template_template_group_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY template_group_template
    ADD CONSTRAINT template_group_template_template_group_id_fkey
    FOREIGN KEY (template_group_id)
    REFERENCES template_group(hid);

--
-- Name: template_group_template_template_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY template_group_template
    ADD CONSTRAINT template_group_template_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES transformation_activity(hid);

--
-- Name: template_input_input_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY template_input
    ADD CONSTRAINT template_input_input_id_fkey
    FOREIGN KEY (input_id)
    REFERENCES dataobjects(hid);

--
-- Name: template_input_template_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY template_input
    ADD CONSTRAINT template_input_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES template(hid);

--
-- Name: template_output_output_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY template_output
    ADD CONSTRAINT template_output_output_id_fkey
    FOREIGN KEY (output_id)
    REFERENCES dataobjects(hid);

--
-- Name: template_output_template_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY template_output
    ADD CONSTRAINT template_output_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES template(hid);

--
-- Name: template_property_group_property_group_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY template_property_group
    ADD CONSTRAINT template_property_group_property_group_id_fkey
    FOREIGN KEY (property_group_id)
    REFERENCES property_group(hid);

--
-- Name: template_property_group_template_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY template_property_group
    ADD CONSTRAINT template_property_group_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES template(hid);

--
-- Name: trans_act_integration_data_id_fk; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY transformation_activity
    ADD CONSTRAINT trans_act_integration_data_id_fk
    FOREIGN KEY (integration_data_id)
    REFERENCES integration_data(id);

--
-- Name: transformation_activity_mapping_transformation_activity_id_fkey;
-- Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_activity_mappings
    ADD CONSTRAINT transformation_activity_mapping_transformation_activity_id_fkey
    FOREIGN KEY (transformation_activity_id)
    REFERENCES transformation_activity(hid);

--
-- Name: transformation_activity_mappings_mapping_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_activity_mappings
    ADD CONSTRAINT transformation_activity_mappings_mapping_id_fkey
    FOREIGN KEY (mapping_id)
    REFERENCES input_output_mapping(hid);

--
-- Name: transformation_activity_referen_transformation_activity_id_fkey;
-- Type: FK CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_activity_references
    ADD CONSTRAINT transformation_activity_referen_transformation_activity_id_fkey
    FOREIGN KEY (transformation_activity_id)
    REFERENCES transformation_activity(hid);

--
-- Name: transformation_step_transformation_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_steps
    ADD CONSTRAINT transformation_step_transformation_id_fkey
    FOREIGN KEY (transformation_id)
    REFERENCES transformations(id);

--
-- Name: transformation_task_steps_transformation_step_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_task_steps
    ADD CONSTRAINT transformation_task_steps_transformation_step_id_fkey
    FOREIGN KEY (transformation_step_id)
    REFERENCES transformation_steps(id);

--
-- Name: transformation_task_steps_transformation_task_id_fkey; Type: FK
-- CONSTRAINT; Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_task_steps
    ADD CONSTRAINT transformation_task_steps_transformation_task_id_fkey
    FOREIGN KEY (transformation_task_id)
    REFERENCES transformation_activity(hid);

--
-- Name: transformation_value_transformation_id_fkey; Type: FK CONSTRAINT;
-- Schema: public; Owner: de
--
ALTER TABLE ONLY transformation_values
    ADD CONSTRAINT transformation_value_transformation_id_fkey
    FOREIGN KEY (transformation_id)
    REFERENCES transformations(id);

--
-- Name: validator_rule_rule_id_fkey; Type: FK CONSTRAINT; Schema: public;
-- Owner: de
--
ALTER TABLE ONLY validator_rule
    ADD CONSTRAINT validator_rule_rule_id_fkey
    FOREIGN KEY (rule_id)
    REFERENCES rule(hid);

--
-- Name: validator_rule_validator_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY validator_rule
    ADD CONSTRAINT validator_rule_validator_id_fkey
    FOREIGN KEY (validator_id)
    REFERENCES validator(hid);

--
-- Name: workspace_root_analysis_group_id_fkey; Type: FK CONSTRAINT; Schema:
-- public; Owner: de
--
ALTER TABLE ONLY workspace
    ADD CONSTRAINT workspace_root_analysis_group_id_fkey
    FOREIGN KEY (root_analysis_group_id)
    REFERENCES template_group(hid);

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
    PRIMARY KEY (id);

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
-- Foreign key constraint for the data_source field of the dataobjects table.
--
ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_data_source_id_fkey
    FOREIGN KEY (data_source_id)
    REFERENCES data_source(id);

--
-- Foreign key constraint for the tool_type_id field of the deployed_components
-- table.
--
ALTER TABLE ONLY deployed_components
    ADD CONSTRAINT deployed_components_tool_type_id_fkey
    FOREIGN KEY (tool_type_id)
    REFERENCES tool_types(id);

--
-- Foreign key constraint for the tool_type_id field of the
-- tool_type_property_type table.
--
ALTER TABLE ONLY tool_type_property_type
    ADD CONSTRAINT tool_type_property_type_tool_type_id_fkey
    FOREIGN KEY (tool_type_id)
    REFERENCES tool_types(id);

--
-- Foreign key constraint for the property_type_id field of the
-- tool_type_property_type table.
--
ALTER TABLE ONLY tool_type_property_type
    ADD CONSTRAINT tool_type_property_type_property_type_fkey
    FOREIGN KEY (property_type_id)
    REFERENCES property_type(hid);

--
-- Foreign key constraint for the requestor_id field of the
-- tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_requestor_id_fkey
    FOREIGN KEY (requestor_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the deployed_component_id field of the
-- tool_requests table.
--
ALTER TABLE ONLY tool_requests
    ADD CONSTRAINT tool_requests_deployed_component_id_fkey
    FOREIGN KEY (deployed_component_id)
    REFERENCES deployed_components(hid);

--
-- Foreign key constraint for the updater_id field of the tool_request_statuses
-- table.
--
ALTER TABLE ONLY tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_updater_id_fkey
    FOREIGN KEY (updater_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the user_id field of the logins table.
--
ALTER TABLE ONLY logins
    ADD CONSTRAINT logins_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Foreign key constraint for the user_id field of the jobs table.
--
ALTER TABLE ONLY jobs
    ADD CONSTRAINT jobs_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

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
-- The primary key for the integrated_webapps table.
--
ALTER TABLE ONLY integrated_webapps
    ADD CONSTRAINT integrated_webapps_pkey
    PRIMARY KEY (id);

--
-- Uniqueness constraint on the name field of the integrated_webapps table.
--
ALTER TABLE ONLY integrated_webapps
    ADD CONSTRAINT integrated_webapps_unique_name
    UNIQUE (name);

--
-- The primary key for the authorization_tokens table.
--
ALTER TABLE ONLY authorization_tokens
    ADD CONSTRAINT authorization_tokens_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the webapp_id column of the authorization_tokens
-- table.
--
ALTER TABLE ONLY authorization_tokens
    ADD CONSTRAINT authorization_tokens_webapp_id_fkey
    FOREIGN KEY (webapp_id)
    REFERENCES integrated_webapps(id);

--
-- Foreign key constraint for the user_id column of the authorization_tokens
-- table.
--
ALTER TABLE ONLY authorization_tokens
    ADD CONSTRAINT authorization_tokens_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id);

--
-- Uniqueness constraint on the webapp_id and user_id columns of the
-- authorization_tokens table.
--
ALTER TABLE ONLY authorization_tokens
    ADD CONSTRAINT authorization_tokens_unique_webapp_id_and_user_id
    UNIQUE (webapp_id, user_id);
