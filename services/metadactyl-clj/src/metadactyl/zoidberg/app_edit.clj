(ns metadactyl.zoidberg.app-edit
  (:use [clojure.string :only [blank?]]
        [korma.core]
        [korma.db :only [transaction]]
        [kameleon.app-groups :only [add-app-to-category get-app-subcategory-id]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.metadata.params :only [format-reference-genome-value]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config :only [workspace-dev-app-group-index]]
        [metadactyl.util.conversions :only [remove-nil-vals convert-rule-argument]]
        [metadactyl.validation :only [validate-parameter verify-app-editable verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as cc-errs]
            [metadactyl.persistence.app-metadata :as persistence]
            [metadactyl.util.service :as service]))

(def ^:private copy-prefix "Copy of ")
(def ^:private max-app-name-len 255)

(defn- get-app-details
  "Retrieves the details for a single-step app."
  [app-id]
  (first (select apps
           (fields :id
                   :name
                   :description
                   :integration_date
                   :edited_date)
           (with app_references)
           (with tasks
             (fields :id)
             (with parameter_groups
               (order :display_order)
               (fields :id
                       :name
                       :description
                       :label
                       [:is_visible :isVisible])
               (with parameters
                 (order :display_order)
                 (with file_parameters
                   (with info_type)
                   (with data_formats)
                   (with data_source))
                 (with parameter_types
                   (with value_type))
                 (with validation_rules
                   (with rule_type)
                   (with validation_rule_arguments
                     (order :ordering)
                     (fields :argument_value))
                   (fields :id
                           [:rule_type.name :type])
                   (where {:rule_type.deprecated false}))
                 (with parameter_values
                       (fields :id
                               :parent_id
                               :name
                               :value
                               [:label :display]
                               :description
                               [:is_default :isDefault])
                       (order [:parent_id :display_order] :ASC))
                 (fields :id
                         :name
                         :label
                         :description
                         [:ordering :order]
                         :required
                         [:is_visible :isVisible]
                         :omit_if_blank
                         [:parameter_types.name :type]
                         [:value_type.name :value_type]
                         [:info_type.name :file_info_type]
                         :file_parameters.is_implicit
                         :file_parameters.repeat_option_flag
                         :file_parameters.retain
                         [:data_source.name :data_source]
                         [:data_formats.name :format]))))
           (where {:id app-id}))))

(defn- format-validator
  [validator]
  {:type (:type validator)
   :params (map (comp convert-rule-argument :argument_value)
                (:validation_rule_arguments validator))})

(defn- format-param-value
  [param-value]
  (remove-nil-vals
    (dissoc param-value :parent_id)))

(defn- format-tree-param-children
  [param-map group]
  (let [id (:id group)
        [groups args] ((juxt filter remove) #(get param-map (:id %)) (get param-map id))
        groups (map format-param-value groups)
        args (map format-param-value args)]
    (assoc group
           :groups (map (partial format-tree-param-children param-map) groups)
           :arguments args)))

(defn- format-tree-params
  [param-values]
  (let [param-map (group-by :parent_id param-values)
        root (first (get param-map nil))
        root {:id (:id root)
              :selectionCascade (:name root)
              :isSingleSelect (:isDefault root)}]
    (format-tree-param-children param-map root)))

(defn- format-list-type-params
  [param-type param-values]
  (if (= param-type persistence/param-tree-type)
    [(format-tree-params param-values)]
    (map format-param-value param-values)))

(defn- format-list-param
  [param param-values]
  (let [param-type (:type param)
        param-args (format-list-type-params param-type param-values)
        param (if-not (= param-type persistence/param-tree-type)
                (assoc param :defaultValue (first (filter :isDefault param-args)))
                param)]
    (assoc param :arguments param-args)))

(defn- format-file-params
  "Returns param with a file_parameters key/value map added if the param type matches one in the
   persistence/param-file-types set. Only includes the repeat_option_flag key in the file_parameters
   map if the param type is also the persistence/param-multi-input-type string."
  [{param-type :type :as param}]
  (let [file-param-keys [:format
                         :file_info_type
                         :is_implicit
                         :data_source
                         :retain]
        file-param-keys (if (= persistence/param-multi-input-type param-type)
                          (conj file-param-keys :repeat_option_flag)
                          file-param-keys)]
    (if (contains? persistence/param-file-types param-type)
      (assoc param :file_parameters (select-keys param file-param-keys))
      param)))

(defn- format-default-value
  [{param-type :type :as param} default-value]
  (let [default-value (if (and default-value
                               (contains? persistence/param-reference-genome-types param-type))
                        (format-reference-genome-value default-value)
                        default-value)]
    (assoc param :defaultValue default-value)))

(defn- format-param
  [{param-type :type
    value-type :value_type
    param-values :parameter_values
    validation-rules :validation_rules
    :as param}]
  (when-not value-type
    (throw+ {:error_code cc-errs/ERR_NOT_WRITEABLE
             :message "App contains Parameters that cannot be copied or modified at this time."}))
  (let [param (-> param
                  format-file-params
                  (assoc :validators (map format-validator validation-rules))
                  (dissoc :value_type
                          :parameter_values
                          :validation_rules
                          :format
                          :file_info_type
                          :is_implicit
                          :repeat_option_flag
                          :data_source
                          :retain)
                  remove-nil-vals)]
    (if (contains? persistence/param-list-types param-type)
      (format-list-param param param-values)
      (format-default-value param (-> param-values first :value)))))

(defn- format-group
  [group]
  (remove-nil-vals
    (update-in group [:parameters] (partial map format-param))))

(defn- format-app-for-editing
  [app]
  (let [app (get-app-details (:id app))
        task (first (:tasks app))]
    (when (empty? task)
      (throw+ {:error_code cc-errs/ERR_NOT_WRITEABLE
               :message "App contains no steps and cannot be copied or modified."}))
    (remove-nil-vals
      (-> app
          (assoc :references (map :reference_text (:app_references app))
                 :tools      (map remove-nil-vals (persistence/get-app-tools (:id app)))
                 :groups     (map format-group (:parameter_groups task)))
          (dissoc :app_references
                  :tasks)))))

(defn get-app-ui
  "This service prepares a JSON response for editing an App in the client."
  [app-id]
  (let [app (persistence/get-app app-id)]
    (verify-app-ownership app)
    (service/success-response (format-app-for-editing app))))

(defn- update-parameter-argument
  "Adds a selection parameter's argument, and any of its child arguments and groups."
  [param-id parent-id display-order {param-value-id :id groups :groups arguments :arguments :as parameter-value}]
  (let [insert-values (remove-nil-vals
                        (assoc parameter-value :id (uuidify param-value-id)
                                               :parameter_id param-id
                                               :parent_id parent-id
                                               :display_order display-order))
        param-value-id (:id (persistence/add-app-parameter-value insert-values))
        update-sub-arg-mapper (partial update-parameter-argument param-id param-value-id)]
    (remove-nil-vals
        (assoc parameter-value
          :id        param-value-id
          :arguments (when arguments (doall (map-indexed update-sub-arg-mapper arguments)))
          :groups    (when groups    (doall (map-indexed update-sub-arg-mapper groups)))))))

(defn- update-parameter-tree-root
  "Adds a tree selection parameter's root and its child arguments and groups."
  [param-id {name :selectionCascade is-default :isSingleSelect :as root}]
  (let [root (update-parameter-argument param-id nil 0 (assoc root :name name :is_default is-default))]
    (dissoc root :name :is_default)))

(defn- update-param-selection-arguments
  "Adds a selection parameter's arguments."
  [param-type param-id arguments]
  (if (= persistence/param-tree-type param-type)
    [(update-parameter-tree-root param-id (first arguments))]
    (doall (map-indexed (partial update-parameter-argument param-id nil) arguments))))

(defn- save-file-parameter
  "Save an App parameter's file settings."
  [param-type {:keys [retain] :as file-parameter}]
  (let [retain? (if (contains? persistence/param-output-types param-type)
                  true
                  retain)]
    (persistence/add-file-parameter (remove-nil-vals (assoc file-parameter :retain retain?)))))

(defn- add-validation-rule
  "Adds an App parameter's validator and its rule arguments."
  [parameter-id {validator-type :type rule-args :params}]
  (let [validation-rule-id (:id (persistence/add-validation-rule parameter-id validator-type))]
    (dorun (map-indexed (partial persistence/add-validation-rule-argument validation-rule-id)
                        rule-args))))

(defn- update-app-parameter
  "Adds or updates an App parameter and any associated file parameters, validators, and arguments."
  [task-id group-id display-order {param-id :id
                                   default-value :defaultValue
                                   param-type :type
                                   file-parameter :file_parameters
                                   validators :validators
                                   arguments :arguments
                                   visible :isVisible
                                   :or {visible true}
                                   :as parameter}]
  (validate-parameter parameter)
  (let [update-values (assoc parameter :parameter_group_id group-id
                                       :display_order display-order
                                       :isVisible visible)
        param-exists (and param-id (persistence/get-app-parameter param-id task-id))
        param-id (if param-exists
                   param-id
                   (:id (persistence/add-app-parameter update-values)))
        parameter (assoc parameter :id param-id)
        default-value (if (contains? persistence/param-reference-genome-types param-type)
                        (:id default-value)
                        default-value)]
    (when param-exists
      (persistence/update-app-parameter update-values)
      (persistence/remove-file-parameter param-id)
      (persistence/remove-parameter-validation-rules param-id)
      (when-not (contains? persistence/param-file-types param-type)
        (persistence/remove-parameter-mappings param-id)))

    (when-not (or (contains? persistence/param-list-types param-type) (blank? (str default-value)))
      (persistence/add-parameter-default-value param-id default-value))

    (dorun (map (partial add-validation-rule param-id) validators))

    (when (contains? persistence/param-file-types param-type)
      (save-file-parameter param-type (assoc file-parameter :parameter_id param-id)))

    (remove-nil-vals
        (assoc parameter
          :arguments (when (contains? persistence/param-list-types param-type)
                       (update-param-selection-arguments param-type param-id arguments))))))

(defn- update-app-group
  "Adds or updates an App group and its parameters."
  [task-id display-order {group-id :id parameters :parameters :as group}]
  (let [update-values (assoc group :task_id task-id :display_order display-order)
        group-exists (and group-id (persistence/get-app-group group-id task-id))
        group-id (if group-exists
                   group-id
                   (:id (persistence/add-app-group update-values)))]
    (when group-exists
      (persistence/update-app-group update-values))
    (assoc group
      :id group-id
      :parameters (doall (map-indexed (partial update-app-parameter task-id group-id) parameters)))))

(defn- delete-app-parameter-orphans
  "Deletes parameters no longer associated with an App group."
  [{group-id :id params :parameters}]
  (let [parameter-ids (remove nil? (map :id params))]
    (if (empty? parameter-ids)
      (persistence/clear-group-parameters group-id)
      (persistence/remove-parameter-orphans group-id parameter-ids))))

(defn- delete-app-orphans
  "Deletes groups and parameters no longer associated with an App."
  [task-id groups]
  (let [group-ids (remove nil? (map :id groups))]
    (when-not (empty? group-ids)
      (persistence/remove-app-group-orphans task-id group-ids)
      (dorun (map delete-app-parameter-orphans groups)))))

(defn- update-app-groups
  "Adds or updates the given App groups under the given App task ID."
  [task-id groups]
  (let [updated-groups (doall (map-indexed (partial update-app-group task-id) groups))]
    (delete-app-orphans task-id updated-groups)
    updated-groups))

(defn update-app
  "This service will update a single-step App, including the information at its top level and the
   tool used by its single task, as long as the App has not been submitted for public use."
  [{app-id :id :keys [references groups] :as app}]
  (verify-app-editable (persistence/get-app app-id))
  (transaction
    (persistence/update-app app)
    (let [tool-id (->> app :tools first :id)
          app-task (->> (get-app-details app-id) :tasks first)
          task-id (:id app-task)
          current-param-ids (map :id (mapcat :parameters (:parameter_groups app-task)))]
      ;; CORE-6266 prevent duplicate key errors from reused param value IDs
      (when-not (empty? current-param-ids)
        (persistence/remove-parameter-values current-param-ids))
      (when-not (empty? references)
        (persistence/set-app-references app-id references))
      (when-not (nil? tool-id)
        (persistence/set-task-tool task-id tool-id))
      (service/success-response (assoc app :groups (update-app-groups task-id groups))))))

(defn add-app-to-user-dev-category
  "Adds an app with the given ID to the current user's apps-under-development category."
  [app-id]
  (let [workspace-category-id (:root_category_id (get-workspace))
        dev-group-id (get-app-subcategory-id workspace-category-id (workspace-dev-app-group-index))]
    (add-app-to-category app-id dev-group-id)))

(defn- add-single-step-task
  "Adds a task as a single step to the given app, using the app's name, description, and label."
  [{app-id :id :as app}]
  (let [task (persistence/add-task app)]
    (persistence/add-step app-id 0 {:task_id (:id task)})
    task))

(defn add-app
  "This service will add a single-step App, including the information at its top level."
  [{:keys [references groups] :as app}]
  (transaction
    (let [app-id (:id (persistence/add-app app))
          tool-id (->> app :tools first :id)
          task-id (-> (assoc app :id app-id)
                      (add-single-step-task)
                      (:id))]
      (add-app-to-user-dev-category app-id)
      (when-not (empty? references)
        (persistence/set-app-references app-id references))
      (when-not (nil? tool-id)
        (persistence/set-task-tool task-id tool-id))
      (dorun (map-indexed (partial update-app-group task-id) groups))
      (get-app-ui app-id))))

(defn- name-too-long?
  "Determines if a name is too long to be extended for a copy name."
  [original-name]
  (> (+ (count copy-prefix) (count original-name)) max-app-name-len))

(defn- already-copy-name?
  "Determines if the name of an app is already a copy name."
  [original-name]
  (.startsWith original-name copy-prefix))

(defn app-copy-name
  "Determines the name of a copy of an app."
  [original-name]
  (cond (name-too-long? original-name)     original-name
        (already-copy-name? original-name) original-name
        :else                              (str copy-prefix original-name)))
