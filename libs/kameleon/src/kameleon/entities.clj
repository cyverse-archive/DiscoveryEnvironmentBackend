(ns kameleon.entities
  (:use [korma.core]))

(declare users collaborator requestor workspace template_group transformation_activity
         transformation_activity_references integration_data deployed_components
         deployed_component_data_files transformation_steps transformations
         output_mapping input_mapping template inputs outputs info_type
         data_formats multiplicity property_group property property_type
         value_type validator rule rule_type rule_subtype analysis_group_listing
         analysis_listing deployed_component_listing dataelementpreservation
         importedworkflow notification_set notification ratings collaborators
         genome_reference created_by last_modified_by data_source tool_types
         tool_request_status_codes tool_architectures tool_requests
         tool_request_statuses)

;; Users who have logged into the DE.  Multiple entities are associated with
;; the same table in order to allow us to have multiple relationships between
;; the same two tables.
(defentity users
  (entity-fields :username)
  (has-one workspace {:fk :user_id})
  (has-many ratings {:fk :user_id}))
(defentity collaborator
  (table :users :collaborator)
  (entity-fields :username)
  (has-many collaborators {:fk :collaborator_id}))

;; The workspaces of users who have logged into the DE.
(defentity workspace
  (entity-fields :is_public)
  (belongs-to users {:fk :user_id})
  (belongs-to template_group {:fk :root_analysis_group_id}))

;; An app group.
(defentity template_group
  (pk :hid)
  (entity-fields :id :name :description)
  (belongs-to workspace)
  (many-to-many template_group :template_group_group
                {:lfk :parent_group_id
                 :rfk :subgroup_id})
  (many-to-many transformation_activity :template_group_template
                {:lfk :template_group_id
                 :rfk :template_id}))

;; An app.
(defentity transformation_activity
  (pk :hid)
  (entity-fields :id :name :location :description :type :deleted :wikiurl
                 :integration_date :disabled :edited_date)
  (belongs-to workspace)
  (belongs-to integration_data)
  (many-to-many template_group :template_group_template
                {:lfk :template_id
                 :rfk :template_group_id})
  (many-to-many transformation_steps :transformation_task_steps
                {:lfk :transformation_task_id
                 :rfk :transformation_step_id})
  (has-many transformation_activity_references)
  (has-many ratings))

;; References associated with an app.
(defentity transformation_activity_references
  (entity-fields :reference_text))

;; Information about who integrated an app or a deployed component.
(defentity integration_data
  (entity-fields :integrator_name :integrator_email)
  (has-many transformation_activity)
  (has-many deployed_components))

;; Information about a deployed tool.
(defentity deployed_components
  (pk :hid)
  (entity-fields :id :name :location :description :version :attribution)
  (belongs-to integration_data)
  (belongs-to tool_types {:fk :tool_type_id})
  (has-many deployed_component_data_files {:fk :deployed_component_id})
  (has-many tool_requests {:fk :deployed_component_id}))

;; Test data files for use with deployed components.
(defentity deployed_component_data_files
  (entity-fields :filename :input_file)
  (belongs-to deployed_components {:fk :deployed_component_id}))

;; Steps within an app.
(defentity transformation_steps
  (entity-fields :name :guid :description)
  (belongs-to transformations {:fk :transformation_id})
  (has-many output_mapping {:fk :source})
  (has-many input_mapping {:fk :target}))

;; Transformations applied to steps within an app.
(defentity transformations
  (entity-fields :name :description :template_id :external_app_id))

;; A table that maps outputs from one step to inputs to another set.  Two
;; entities are associated with a single table here for convenience.  when I
;; have more time, I'd like to try to improve the relation handling in Korma
;; so that multiple relationships with the same table work correctly.
(defentity output_mapping
  (pk :hid)
  (table :input_output_mapping :output_mapping))
(defentity input_mapping
  (pk :hid)
  (table :input_output_mapping :input_mapping))

;; Data object mappings can't be implemeted as entities until Korma supports
;; composite primary keys.  In the meantime, we'll have to deal with this table
;; in code.

;; A template defines an interface to a tool that can be called.
(defentity template
  (pk :hid)
  (entity-fields :id :name :description :label :type :component_id)
  (many-to-many inputs :template_input
                {:lfk :template_id
                 :rfk :input_id})
  (many-to-many outputs :template_output
                {:lfk :template_id
                 :rfk :output_id})
  (many-to-many property_group :template_property_group
                {:lfk :template_id
                 :rfk :property_group_id}))

;; Input and output definitions.  Once again, multiple entities are associated
;; with the same table to allow us to define multiple relationships between
;; the same two tables.
(defentity inputs
  (pk :hid)
  (table :dataobjects :inputs)
  (entity-fields :id :name :label :orderd :switch :description :required
                 :retain :is_implicit)
  (belongs-to info_type {:fk :info_type})
  (belongs-to data_formats {:fk :data_format})
  (belongs-to multiplicity {:fk :multiplicity})
  (belongs-to data_source {:fk :data_source_id}))
(defentity outputs
  (pk :hid)
  (table :dataobjects :outputs)
  (entity-fields :id :name :label :orderd :switch :description :required
                 :retain :is_implicit)
  (belongs-to info_type {:fk :info_type})
  (belongs-to data_formats {:fk :data_format})
  (belongs-to multiplicity {:fk :multiplicity})
  (belongs-to data_source {:fk :data_source_id}))
(defentity data_object
  (pk :hid)
  (table :dataobjects :data_object)
  (entity-fields :id :name :label :orderd :switch :description :required
                 :retain :is_implicit)
  (belongs-to info_type {:fk :info_type})
  (belongs-to data_formats {:fk :data_format})
  (belongs-to multiplicity {:fk :multiplicity})
  (belongs-to data_source {:fk :data_source_id}))

;; The type of information stored in a data object.
(defentity info_type
  (pk :hid)
  (entity-fields :id :name :label :description :deprecated :display_order))

;; The format of the data in a data object.
(defentity data_formats
  (entity-fields :guid :name :label :display_order))

;; An input or output multiplicity definition.
(defentity multiplicity
  (pk :hid)
  (entity-fields :id :name :label :description :type_name))

;; A group of properties.
(defentity property_group
  (pk :hid)
  (entity-fields :id :name :description :label :group_type :is_visible)
  (many-to-many property :property_group_property
                {:lfk :property_group_id
                 :rfk :property_id}))

;; A single property.
(defentity property
  (pk :hid)
  (entity-fields :id :name :description :label :defalut_value :is_visible
                 :ordering :omit_if_blank)
  (belongs-to data_object {:fk :dataobject_id})
  (belongs-to property_type {:fk :property_type})
  (belongs-to validator {:fk :validator})
  (many-to-many tool_types :tool_type_property_type
                {:lfk :property_type_id
                 :rfk :tool_type_id}))

;; The type of a single property.
(defentity property_type
  (pk :hid)
  (entity-fields :id :name :description :label :deprecated :display_order)
  (belongs-to value_type))

;; The type of value associated with a property.  This is used to determine
;; which rule types may be associated with a property.
(defentity value_type
  (pk :hid)
  (entity-fields :id :name :description)
  (has-one property_type)
  (many-to-many rule_type :rule_type_value_type
                {:lfk :value_type_id
                 :rfk :rule_type_id}))

;; Validators are used to describe how a property should be validated.
(defentity validator
  (pk :hid)
  (entity-fields :id :name :description :label :required)
  (many-to-many rule :validator_rule
                {:lfk :validator_id
                 :rfk :rule_id}))

;; Rules are used to describe individual validation steps for a property.
(defentity rule
  (pk :hid)
  (entity-fields :id :name :description :label)
  (belongs-to rule_type {:fk :rule_type}))

;; Rule types indicate the validation method to use.
(defentity rule_type
 (pk :hid)
  (entity-fields :id :name :description :label :deprecated :display_order
                 :rule_description_format)
  (belongs-to rule_subtype)
  (many-to-many value_type :rule_type_value_type
                {:lfk :rule_type_id}
                {:rfk :value_type_id}))

;; Rule arguments will have to be handled in code until Korma can be enhanced
;; to accept composite primary keys.

;; Rule subtypes are used to distinguish different flavors of values that
;; rules can be applied to.  For example, Number value types are segregated
;; into Integer and Double subtypes.
(defentity rule_subtype
  (pk :hid)
  (entity-fields :id :name :description))

;; A view used to list analysis groups.
(defentity analysis_group_listing
  (pk :hid)
  (entity-fields :id :name :description :workspace_id :is_public)
  (many-to-many analysis_group_listing :template_group_group
                {:lfk :parent_group_id
                 :rfk :subgroup_id})
  (many-to-many analysis_listing :template_group_template
                {:lfk :template_group_id
                 :rfk :template_id}))

;; A view used to list analyses.
(defentity analysis_listing
  (pk :hid)
  (entity-fields :id :name :description :integrator_name :integrator_email
                 :integration_date :edited_date :wikiurl :average_rating
                 :is_public :step_count :component_count :deleted :disabled
                 :overall_job_type)
  (has-many deployed_component_listing {:fk :analysis_id})
  (has-many ratings {:fk :transformation_activity_id}))

;; A view used to list deployed components.
(defentity deployed_component_listing
  (entity-fields :analysis_id :execution_order :deployed_component_hid
                 :deployed_component_id :name :description :location :type
                 :version :attribution))

;; Retained information about data objects.
(defentity dataelementpreservation
  (pk :hid)
  (entity-fields :templateuuid :infotypename :fileformatname :datecreated))

;; Records of workflow metadata elements that have been imported.
(defentity importedworkflow
  (pk :hid)
  (entity-fields :importjson :analysisuuid :datecreated))

;; Notification sets are groups of notifications.
(defentity notification_set
  (pk :hid)
  (entity-fields :id :name :template_id)
  (many-to-many notification :notification_set_notification
                {:lfk :notification_set_id
                 :rfk :notification_id}))

;; Notifications are used to coordinate UI panels in an analysis.
(defentity notification
  (pk :hid)
  (entity-fields :id :name :sender :type))

;; Notification receivers will have to be handled in code until Korma can be
;; enhanced to allow composite primary keys.

;; Application ratings.
(defentity ratings
  (entity-fields :rating :comment_id)
  (belongs-to users {:fk :user_id})
  (belongs-to transformation_activity))

;; A view for listing rating information.
(defentity rating_listing
  (entity-fields :analysis_id :user_id :comment_id :user_rating)
  (belongs-to transformation_activity {:fk :analysis_id})
  (belongs-to users {:fk :user_id}))

;; Database version entries.
(defentity version
  (entity-fields :version :applied))

;; Associates users with other users for collaboration.
(defentity collaborators
  (belongs-to users {:fk :user_id})
  (belongs-to collaborator {:fk :collaborator_id}))

;; Contains genomic metadata.
(defentity genome_reference
  (table :genome_reference)
  (entity-fields :uuid :name :path :deleted :created_on :last_modified_on)
  (belongs-to created_by {:fk :created_by})
  (belongs-to last_modified_by {:fk :last_modified_by}))
(defentity created_by
  (table :users :created_by)
  (entity-fields :username)
  (has-one genome_reference {:fk :created_by}))
(defentity last_modified_by
  (table :users :last_modified_by)
  (entity-fields :username)
  (has-one genome_reference {:fk :last_modified_by}))

;; Data source.
(defentity data_source
  (table :data_source)
  (entity-fields :uuid :name :label :description))

;; Tool types.
(defentity tool_types
  (table :tool_types)
  (entity-fields :name :description)
  (many-to-many property_type :tool_type_property_type
                {:lfk :tool_type_id
                 :rfk :property_type_id}))

;; Tool request status codes.
(defentity tool_request_status_codes
  (table :tool_request_status_codes)
  (entity-fields :name :description)
  (has-many tool_request_statuses {:fk :tool_request_status_code_id}))

;; Tool architectures.
(defentity tool_architectures
  (table :tool_architectures)
  (entity-fields :name :description)
  (has-many tool_requests {:fk :tool_architecture_id}))

;; The user who submitted a tool request.
(defentity requestor
  (table :users :requestor)
  (entity-fields :username)
  (has-many tool_requests {:fk :requestor_id}))

;; Tool requests.
(defentity tool_requests
  (table :tool_requests)
  (entity-fields
   :uuid :phone :tool_name :description :source_url :doc_url :version :attribution :multithreaded
   :test_data_path :instructions :additional_info :additional_data_file)
  (belongs-to requestor {:fk :requestor_id})
  (belongs-to tool_architectures {:fk :tool_architecture_id})
  (belongs-to deployed_components {:fk :deployed_component_id})
  (has-many tool_request_statuses {:fk :tool_request_id}))

;; The user who updated a tool request.
(defentity updater
  (table :users :updater)
  (entity-fields :username)
  (has-many tool_request_statuses {:fk :updater_id}))

;; Tool request status changes.
(defentity tool_request_statuses
  (table :tool_request_statuses)
  (entity-fields :date-assigned :comments)
  (belongs-to tool_requests {:fk :tool_request_id})
  (belongs-to tool_request_status_codes {:fk :tool_request_status_code_id})
  (belongs-to updater {:fk :updater_id}))

(defentity user-preferences
  (table :user_preferences)
  (entity-fields :id :preferences)
  (belongs-to users {:fk :user_id}))

(defentity tree-urls
  (table :tree_urls)
  (entity-fields :id :tree-urls))

(defentity user-sessions
  (table :user_sessions)
  (entity-fields :id :session)
  (belongs-to users {:fk :user_id}))

(defentity user-saved-searches
  (table :user_saved_searches)
  (entity-fields :id :saved_searches)
  (belongs-to users {:fk :user_id}))
