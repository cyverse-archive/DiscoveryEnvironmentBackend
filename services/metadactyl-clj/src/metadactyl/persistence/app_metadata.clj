(ns metadactyl.persistence.app-metadata
  "Persistence layer for app metadata."
  (:use [kameleon.entities]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [korma.db :only [transaction]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions]
        [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [metadactyl.persistence.app-metadata.delete :as delete]
            [metadactyl.persistence.app-metadata.relabel :as relabel]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

(def param-input-types #{"FileInput" "FolderInput" "MultiFileSelector"})
(def param-output-types #{"FileOutput" "FolderOutput" "MultiFileOutput"})
(def param-file-types (set/union param-input-types param-output-types))

(def param-selection-types #{"TextSelection" "DoubleSelection" "IntegerSelection"})
(def param-tree-type "TreeSelection")
(def param-list-types (conj param-selection-types param-tree-type))

(defn- filter-valid-app-values
  "Filters valid keys from the given App for inserting or updating in the database, setting the
   current date as the edited date."
  [app]
  (-> app
      (select-keys [:name :description])
      (assoc :edited_date (sqlfn now))))

(defn- filter-valid-task-values
  "Filters valid keys from the given Task for inserting or updating in the database."
  [task]
  (select-keys task [:name :description :label :tool_id :external_app_id]))

(defn- filter-valid-app-group-values
  "Filters and renames valid keys from the given App group for inserting or updating in the database."
  [group]
  (-> group
      (set/rename-keys {:isVisible :is_visible})
      (select-keys [:id :task_id :name :description :label :display_order :is_visible])))

(defn- filter-valid-app-parameter-values
  "Filters and renames valid keys from the given App parameter for inserting or updating in the
   database."
  [parameter]
  (-> parameter
      (set/rename-keys {:isVisible :is_visible
                        :order :ordering})
      (select-keys [:id
                    :parameter_group_id
                    :name
                    :description
                    :label
                    :is_visible
                    :ordering
                    :display_order
                    :parameter_type
                    :required
                    :omit_if_blank])))

(defn- filter-valid-file-parameter-values
  "Filters valid keys from the given file-parameter for inserting or updating in the database."
  [file-parameter]
  (select-keys file-parameter [:parameter_id
                               :retain
                               :is_implicit
                               :info_type
                               :data_format
                               :data_source_id]))

(defn- filter-valid-parameter-value-values
  "Filters and renames valid keys from the given parameter-value for inserting or updating in the
   database."
  [parameter-value]
  (-> parameter-value
      (set/rename-keys {:display :label
                        :isDefault :is_default})
      (select-keys [:id
                    :parameter_id
                    :parent_id
                    :is_default
                    :display_order
                    :name
                    :value
                    :description
                    :label])))

(defn get-app
  "Retrieves all app listing fields from the database."
  [app-id]
  (assert-not-nil [:app-id app-id] (first (select app_listing (where {:id app-id})))))

(defn get-integration-data
  "Retrieves integrator info from the database, adding it first if not already there."
  ([{:keys [email first-name last-name]}]
   (get-integration-data email (str first-name " " last-name)))
  ([integrator-email integrator-name]
   (if-let [integration-data (first (select integration_data
                                      (where {:integrator_email integrator-email})))]
     integration-data
     (insert integration_data (values {:integrator_email integrator-email
                                       :integrator_name  integrator-name})))))

(defn get-app-tools
  "Loads information about the tools associated with an app."
  [app-id]
  (select tool_listing
          (fields [:tool_id :id]
                  :name
                  :description
                  :location
                  :type
                  :version
                  :attribution)
          (where {:app_id app-id})))

(defn get-tool-type-id
  "Gets the ID of the given tool type name."
  [tool-type]
  (:id (first (select tool_types (fields :id) (where {:name tool-type})))))

(defn add-tool-data-file
  "Adds a tool's test data files to the database"
  [tool-id input-file filename]
  (insert tool_test_data_files (values {:tool_id tool-id
                                        :input_file input-file
                                        :filename filename})))

(defn add-tool
  "Adds a new tool and its test data files to the database"
  [{type :type {:keys [implementor_email implementor test]} :implementation :as tool}]
  (transaction
    (let [integration-data-id (:id (get-integration-data implementor_email implementor))
          tool (-> tool
                   (select-keys [:id :name :description :attribution :location :version])
                   (assoc :integration_data_id integration-data-id
                          :tool_type_id (get-tool-type-id type)))
          tool-id (:id (insert tools (values tool)))]
      (dorun (map (partial add-tool-data-file tool-id true) (:input_files test)))
      (dorun (map (partial add-tool-data-file tool-id false) (:output_files test)))
      tool-id)))

(defn add-app
  "Adds top-level app info to the database and returns the new app info, including its new ID."
  [app]
  (let [integration-data-id (:id (get-integration-data current-user))
        app (-> app
                (select-keys [:id :name :description])
                (assoc :integration_data_id integration-data-id
                       :edited_date (sqlfn now)))]
    (insert apps (values app))))

(defn add-step
  "Adds an app step to the database for the given app ID."
  [app-id step-number step]
  (let [step (-> step
                 (select-keys [:task_id])
                 (assoc :app_id app-id
                        :step step-number))]
    (insert app_steps (values step))))

(defn- add-workflow-io-map
  [mapping]
  (insert :workflow_io_maps
          (values {:app_id      (:app_id mapping)
                   :source_step (get-in mapping [:source_step :id])
                   :target_step (get-in mapping [:target_step :id])})))

(defn- build-io-key
  [step de-app-key external-app-key value]
  (let [stringify #(if (keyword? %) (name %) %)]
    (if (= (:app_type step) "External")
      [external-app-key (stringify value)]
      [de-app-key       (uuidify value)])))

(defn- io-mapping-builder
  [mapping-id mapping]
  (fn [[input output]]
    (into {} [(vector :mapping_id mapping-id)
              (build-io-key (:target_step mapping) :input :external_input input)
              (build-io-key (:source_step mapping) :output :external_output output)])))

(defn add-mapping
  "Adds an input/output workflow mapping to the database for the given app source->target mapping."
  [mapping]
  (let [mapping-id (:id (add-workflow-io-map mapping))]
    (->> (:map mapping)
         (map (io-mapping-builder mapping-id mapping))
         (map #(insert :input_output_mapping (values %)))
         (dorun))))

(defn update-app
  "Updates top-level app info in the database."
  ([app]
   (update-app app false))
  ([app publish?]
   (let [app-id (:id app)
         app (-> app
                 (select-keys [:name :description :wiki_url :deleted :disabled])
                 (assoc :edited_date (sqlfn now)
                        :integration_date (when publish? (sqlfn now)))
                 (remove-nil-vals))]
     (update apps (set-fields app) (where {:id app-id})))))

(defn add-app-reference
  "Adds an App's reference to the database."
  [app-id reference]
  (insert app_references (values {:app_id app-id, :reference_text reference})))

(defn set-app-references
  "Resets the given App's references with the given list."
  [app-id references]
  (transaction
    (delete app_references (where {:app_id app-id}))
    (dorun (map (partial add-app-reference app-id) references))))

(defn add-app-suggested-category
  "Adds an App's suggested category to the database."
  [app-id category-id]
  (insert :suggested_groups (values {:app_id app-id, :app_category_id category-id})))

(defn set-app-suggested-categories
  "Resets the given App's suggested categories with the given category ID list."
  [app-id category-ids]
  (transaction
    (delete :suggested_groups (where {:app_id app-id}))
    (dorun (map (partial add-app-suggested-category app-id) category-ids))))

(defn add-task
  "Adds a task to the database."
  [task]
  (insert tasks (values (filter-valid-task-values task))))

(defn set-task-tool
  "Sets the given tool-id as the given task's tool."
  [task-id tool-id]
  (update tasks (set-fields {:tool_id tool-id}) (where {:id task-id})))

(defn remove-app-steps
  "Removes all steps from an App. This delete will cascade to workflow_io_maps and
   input_output_mapping entries."
  [app-id]
  (delete app_steps (where {:app_id app-id})))

(defn remove-workflow-map-orphans
  "Removes any orphaned workflow_io_maps table entries."
  []
  (delete :workflow_io_maps
    (where (not (exists (subselect [:input_output_mapping :iom]
                          (where {:iom.mapping_id :workflow_io_maps.id})))))))

(defn remove-parameter-mappings
  "Removes all input-output mappings associated with the given parameter ID, then removes any
   orphaned workflow_io_maps table entries."
  [parameter-id]
  (transaction
    (delete :input_output_mapping (where (or {:input parameter-id}
                                             {:output parameter-id})))
    (remove-workflow-map-orphans)))

(defn update-app-labels
  "Updates the labels in an app."
  [req]
  (relabel/update-app-labels req))

(defn get-app-group
  "Fetches an App group."
  ([group-id]
   (first (select parameter_groups (where {:id group-id}))))
  ([group-id task-id]
   (first (select parameter_groups (where {:id group-id, :task_id task-id})))))

(defn add-app-group
  "Adds an App group to the database."
  [group]
  (insert parameter_groups (values (filter-valid-app-group-values group))))

(defn update-app-group
  "Updates an App group in the database."
  [{group-id :id :as group}]
  (update parameter_groups
    (set-fields (filter-valid-app-group-values group))
    (where {:id group-id})))

(defn remove-app-group-orphans
  "Removes groups associated with the given task ID, but not in the given group-ids list."
  [task-id group-ids]
  (delete parameter_groups (where {:task_id task-id
                                   :id [not-in group-ids]})))

(defn get-parameter-type-id
  "Gets the ID of the given parameter type name."
  [parameter-type]
  (:id (first
         (select parameter_types
           (fields :id)
           (where {:name parameter-type :deprecated false})))))

(defn get-info-type-id
  "Gets the ID of the given info type name."
  [info-type]
  (:id (first
         (select info_type
           (fields :id)
           (where {:name info-type :deprecated false})))))

(defn get-data-format-id
  "Gets the ID of the data format with the given name."
  [data-format]
  (:id (first
         (select data_formats
           (fields :id)
           (where {:name data-format})))))

(defn get-data-source-id
  "Gets the ID of the data source with the given name."
  [data-source]
  (:id (first
         (select data_source
           (fields :id)
           (where {:name data-source})))))

(defn get-app-parameter
  "Fetches an App parameter."
  ([parameter-id]
   (first (select parameters (where {:id parameter-id}))))
  ([parameter-id group-id]
   (first (select parameters (where {:id parameter-id, :parameter_group_id group-id})))))

(defn add-app-parameter
  "Adds an App parameter to the parameters table."
  [{param-type :type :as parameter}]
  (insert parameters
    (values (filter-valid-app-parameter-values
              (assoc parameter :parameter_type (get-parameter-type-id param-type))))))

(defn update-app-parameter
  "Updates a parameter in the parameters table."
  [{parameter-id :id param-type :type :as parameter}]
  (update parameters
    (set-fields (filter-valid-app-parameter-values
                  (assoc parameter :parameter_type (get-parameter-type-id param-type))))
    (where {:id parameter-id})))

(defn remove-parameter-orphans
  "Removes parameters associated with the given group ID, but not in the given parameter-ids list."
  [group-id parameter-ids]
  (delete parameters (where {:parameter_group_id group-id
                             :id [not-in parameter-ids]})))

(defn add-file-parameter
  "Adds file parameter fields to the database."
  [{info-type :file_info_type data-format :format data-source :data_source :as file-parameter}]
  (insert file_parameters
    (values (filter-valid-file-parameter-values
              (assoc file-parameter
                :info_type (get-info-type-id info-type)
                :data_format (get-data-format-id data-format)
                :data_source_id (get-data-source-id data-source))))))

(defn remove-file-parameter
  "Removes all file parameters associated with the given parameter ID."
  [parameter-id]
  (delete file_parameters (where {:parameter_id parameter-id})))

(defn add-validation-rule
  "Adds a validation rule to the database."
  [parameter-id rule-type]
  (insert validation_rules
    (values {:parameter_id parameter-id
             :rule_type    (subselect rule_type
                             (fields :id)
                             (where {:name rule-type
                                     :deprecated false}))})))

(defn remove-parameter-validation-rules
  "Removes all validation rules and rule arguments associated with the given parameter ID."
  [parameter-id]
  (delete validation_rules (where {:parameter_id parameter-id})))

(defn add-validation-rule-argument
  "Adds a validation rule argument to the database."
  [validation-rule-id ordering argument-value]
  (insert validation_rule_arguments (values {:rule_id validation-rule-id
                                             :ordering ordering
                                             :argument_value argument-value})))

(defn add-parameter-default-value
  "Adds a parameter's default value to the database."
  [parameter-id default-value]
  (insert parameter_values (values {:parameter_id parameter-id
                                    :value        default-value
                                    :is_default   true})))

(defn add-app-parameter-value
  "Adds a parameter value to the database."
  [parameter-value]
  (insert parameter_values (values (filter-valid-parameter-value-values parameter-value))))

(defn remove-parameter-values
  "Removes all parameter values associated with the given parameter ID."
  [parameter-id]
  (delete parameter_values (where {:parameter_id parameter-id})))

(defn remove-parameter-value-orphans
  "Removes parameter values associated with the given parameter ID, but not in the given
   parameter-value-ids list."
  [parameter-id parameter-value-ids]
  (delete parameter_values (where {:parameter_id parameter-id
                                   :id [not-in parameter-value-ids]})))

(defn app-accessible-by
  "Obtains the list of users who can access an app."
  [app-id]
  (map :username
       (select [:apps :a]
               (join [:app_category_app :aca]
                     {:a.id :aca.app_id})
               (join [:app_categories :g]
                     {:aca.app_category_id :g.id})
               (join [:workspace :w]
                     {:g.workspace_id :w.id})
               (join [:users :u]
                     {:w.user_id :u.id})
               (fields :u.username)
               (where {:a.id app-id}))))

(defn count-external-steps
  "Counts how many steps have an external ID in the given app."
  [app-id]
  ((comp :count first)
   (select [:app_steps :s]
     (aggregate (count :external_app_id) :count)
     (join [:tasks :t] {:s.task_id :t.id})
     (where {:s.app_id app-id})
     (where (raw "t.external_app_id IS NOT NULL")))))

(defn permanently-delete-app
  "Permanently removes an app from the metadata database."
  [app-id]
  (delete/permanently-delete-app ((comp :id get-app) app-id)))

(defn delete-app
  "Marks or unmarks an app as deleted in the metadata database."
  ([app-id]
   (delete-app true app-id))
  ([deleted? app-id]
   (update :apps (set-fields {:deleted deleted?}) (where {:id app-id}))))

(defn disable-app
  "Marks or unmarks an app as disabled in the metadata database."
  ([app-id]
   (disable-app true app-id))
  ([disabled? app-id]
   (update :apps (set-fields {:disabled disabled?}) (where {:id app-id}))))

(defn rate-app
  "Adds or updates a user's rating and comment ID for the given app."
  [app-id user-id request]
  (let [rating (first (select ratings (where {:app_id app-id, :user_id user-id})))]
    (if rating
      (update ratings
              (set-fields (remove-nil-vals request))
              (where {:app_id app-id
                      :user_id user-id}))
      (insert ratings
              (values (assoc (remove-nil-vals request) :app_id app-id, :user_id user-id))))))

(defn delete-app-rating
  "Removes a user's rating and comment ID for the given app."
  [app-id user-id]
  (delete ratings
    (where {:app_id app-id
            :user_id user-id})))

(defn get-app-avg-rating
  "Gets the average and total number of user ratings for the given app ID."
  [app-id]
  (first
    (select ratings
            (fields (raw "CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION) AS average"))
            (aggregate (count :rating) :total)
            (where {:app_id app-id}))))
