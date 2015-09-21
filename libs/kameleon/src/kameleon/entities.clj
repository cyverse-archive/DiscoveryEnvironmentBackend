(ns kameleon.entities
  (:use [korma.core :exclude [update]]))

(declare users collaborator requestor workspace app_categories apps app_references integration_data
         tools tool_test_data_files output_mapping input_mapping tasks inputs outputs
         task_parameters info_type data_formats multiplicity parameter_groups parameters
         parameter_values parameter_types value_type validation_rules validation_rule_arguments
         rule_type rule_subtype app_category_listing app_listing tool_listing ratings collaborators
         genome_reference created_by last_modified_by data_source tool_types
         tool_request_status_codes tool_architectures tool_requests
         tool_request_statuses container-images container-settings container-devices container-volumes container-volumes-from)

;; Users who have logged into the DE.  Multiple entities are associated with
;; the same table in order to allow us to have multiple relationships between
;; the same two tables.
(defentity users
  (has-one workspace {:fk :user_id})
  (has-many ratings {:fk :user_id}))
(defentity collaborator
  (table :users :collaborator)
  (has-many collaborators {:fk :collaborator_id}))

;; The workspaces of users who have logged into the DE.
(defentity workspace
  (belongs-to users {:fk :user_id})
  (belongs-to app_categories {:fk :root_category_id}))

;; An app group.
(defentity app_categories
  (belongs-to workspace)
  (many-to-many app_categories :app_category_group
                {:lfk :parent_category_id
                 :rfk :child_category_id})
  (many-to-many apps :app_category_app
                {:lfk :app_category_id
                 :rfk :app_id}))

;; An app.
(defentity apps
  (belongs-to integration_data)
  (many-to-many app_categories :app_category_app
                {:lfk :app_id
                 :rfk :app_category_id})
  (many-to-many tasks :app_steps
                {:lfk :app_id
                 :rfk :task_id})
  (has-many app_references {:fk :app_id})
  (has-many ratings {:fk :app_id}))

;; References associated with an app.
(defentity app_references)

;; Information about who integrated an app or a deployed component.
(defentity integration_data
  (has-many apps)
  (has-many tools))

(defentity data-containers
  (table :data_containers)
  (has-one container-volumes-from)
  (belongs-to container-images))

;; Information about containers containing tools.
(defentity container-images
  (table :container_images)
  (has-one data-containers))

(defentity container-settings
  (table :container_settings)
  (belongs-to tools)
  (has-many container-devices)
  (has-many container-volumes)
  (has-many container-volumes-from))

(defentity container-devices
  (table :container_devices)
  (belongs-to container-settings))

(defentity container-volumes
  (table :container_volumes)
  (belongs-to container-settings))

(defentity container-volumes-from
  (table :container_volumes_from)
  (belongs-to container-settings)
  (belongs-to data-containers))

;; Information about a deployed tool.
(defentity tools
  (belongs-to integration_data)
  (belongs-to tool_types {:fk :tool_type_id})
  (has-many tool_test_data_files {:fk :tool_id})
  (has-many tool_requests {:fk :tool_id})
  (has-one container-settings))

;; Test data files for use with deployed components.
(defentity tool_test_data_files
  (belongs-to tools {:fk :tool_id}))

;; Steps within an app.
(defentity app_steps
  (has-many output_mapping {:fk :source_step})
  (has-many input_mapping {:fk :target_step}))

;; A table that maps outputs from one step to inputs to another set.  Two
;; entities are associated with a single table here for convenience.  when I
;; have more time, I'd like to try to improve the relation handling in Korma
;; so that multiple relationships with the same table work correctly.
(defentity output_mapping
  (table :workflow_io_maps :output_mapping))
(defentity input_mapping
  (table :workflow_io_maps :input_mapping))

;; Data object mappings can't be implemeted as entities until Korma supports
;; composite primary keys.  In the meantime, we'll have to deal with this table
;; in code.

;; A task defines an interface to a tool that can be called.
(defentity tasks
  (has-many parameter_groups {:fk :task_id})
  (has-many inputs {:fk :task_id})
  (has-many outputs {:fk :task_id})
  (has-many task_parameters {:fk :task_id}))

;; Input and output definitions. Once again, multiple entities are associated
;; with the same table to allow us to define multiple relationships between
;; the same two tables.
(defentity inputs
  (table (subselect :task_param_listing (where {:value_type "Input"})) :inputs))
(defentity outputs
  (table (subselect :task_param_listing (where {:value_type "Output"})) :outputs))
(defentity task_parameters
  (table :task_param_listing :task_parameters))

;; File parameters.
(defentity file_parameters
  (belongs-to info_type {:fk :info_type})
  (belongs-to data_formats {:fk :data_format})
  (belongs-to multiplicity {:fk :multiplicity})
  (belongs-to data_source {:fk :data_source_id}))

;; The type of information stored in a data object.
(defentity info_type)

;; The format of the data in a data object.
(defentity data_formats)

;; An input or output multiplicity definition.
(defentity multiplicity)

;; A group of parameters.
(defentity parameter_groups
  (has-many parameters {:fk :parameter_group_id}))

;; A single parameter.
(defentity parameters
  (has-many parameter_values {:fk :parameter_id})
  (has-many validation_rules {:fk :parameter_id})
  (has-one file_parameters {:fk :parameter_id})
  (belongs-to parameter_types {:fk :parameter_type})
  (many-to-many tool_types :tool_type_parameter_type
                {:lfk :parameter_type_id
                 :rfk :tool_type_id}))

(defentity parameter_values)

;; The type of a single parameter.
(defentity parameter_types
  (belongs-to value_type))

;; The type of value associated with a parameter.  This is used to determine
;; which rule types may be associated with a parameter.
(defentity value_type
  (has-one parameter_types)
  (many-to-many rule_type :rule_type_value_type
                {:lfk :value_type_id
                 :rfk :rule_type_id}))

;; Validation Rules are used to describe individual validation steps for a parameter.
(defentity validation_rules
  (has-many validation_rule_arguments {:fk :rule_id})
  (belongs-to rule_type {:fk :rule_type}))

;; Rule types indicate the validation method to use.
(defentity rule_type
  (belongs-to rule_subtype)
  (many-to-many value_type :rule_type_value_type
                {:lfk :rule_type_id}
                {:rfk :value_type_id}))

;; Rule arguments will have to be handled in code until Korma can be enhanced
;; to accept composite primary keys.
(defentity validation_rule_arguments)

;; Rule subtypes are used to distinguish different flavors of values that
;; rules can be applied to.  For example, Number value types are segregated
;; into Integer and Double subtypes.
(defentity rule_subtype)

;; A view used to list app categories.
(defentity app_category_listing
  (many-to-many app_category_listing :app_category_group
                {:lfk :parent_category_id
                 :rfk :child_category_id})
  (many-to-many app_listing :app_category_app
                {:lfk :app_category_id
                 :rfk :app_id}))

;; A view used to list apps.
(defentity app_listing
  (has-many tool_listing {:fk :app_id})
  (has-many ratings {:fk :app_id}))

;; A view used to list tools.
(defentity tool_listing)

;; Application ratings.
(defentity ratings
  (belongs-to users {:fk :user_id})
  (belongs-to apps {:fk :app_id}))

;; A view for listing rating information.
(defentity rating_listing
  (belongs-to apps {:fk :app_id})
  (belongs-to users {:fk :user_id}))

;; Database version entries.
(defentity version
  (pk :version))

;; Associates users with other users for collaboration.
(defentity collaborators
  (belongs-to users {:fk :user_id})
  (belongs-to collaborator {:fk :collaborator_id}))

;; Contains genomic metadata.
(defentity genome_reference
  (belongs-to created_by {:fk :created_by})
  (belongs-to last_modified_by {:fk :last_modified_by}))
(defentity created_by
  (table :users :created_by)
  (has-one genome_reference {:fk :created_by}))
(defentity last_modified_by
  (table :users :last_modified_by)
  (has-one genome_reference {:fk :last_modified_by}))

;; Data source.
(defentity data_source)

;; Tool types.
(defentity tool_types
  (many-to-many parameter_types :tool_type_parameter_type
                {:lfk :tool_type_id
                 :rfk :parameter_type_id}))

;; Tool request status codes.
(defentity tool_request_status_codes
  (has-many tool_request_statuses {:fk :tool_request_status_code_id}))

;; Tool architectures.
(defentity tool_architectures
  (has-many tool_requests {:fk :tool_architecture_id}))

;; The user who submitted a tool request.
(defentity requestor
  (table :users :requestor)
  (has-many tool_requests {:fk :requestor_id}))

;; Tool requests.
(defentity tool_requests
  (belongs-to requestor {:fk :requestor_id})
  (belongs-to tool_architectures {:fk :tool_architecture_id})
  (belongs-to tools {:fk :tool_id})
  (has-many tool_request_statuses {:fk :tool_request_id}))

;; The user who updated a tool request.
(defentity updater
  (table :users :updater)
  (has-many tool_request_statuses {:fk :updater_id}))

;; Tool request status changes.
(defentity tool_request_statuses
  (belongs-to tool_requests {:fk :tool_request_id})
  (belongs-to tool_request_status_codes {:fk :tool_request_status_code_id})
  (belongs-to updater {:fk :updater_id}))

(defentity user-preferences
  (table :user_preferences)
  (belongs-to users {:fk :user_id}))

(defentity tree-urls
  (table :tree_urls))

(defentity user-sessions
  (table :user_sessions)
  (belongs-to users {:fk :user_id}))

(defentity user-saved-searches
  (table :user_saved_searches)
  (belongs-to users {:fk :user_id}))
