(ns metadactyl.zoidberg.app-edit
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.app-groups :only [add-app-to-group get-app-subcategory-id]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config :only [workspace-dev-app-group-index]]
        [metadactyl.util.conversions :only [remove-nil-vals convert-rule-argument]]
        [metadactyl.validation :only [verify-app-editable verify-app-ownership]]
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
                         [:data_source.name :data_source]
                         :file_parameters.retain
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
  [{param-type :type :as param}]
  (if (contains? persistence/param-file-types param-type)
    (assoc param :file_parameters (select-keys param [:format
                                                      :file_info_type
                                                      :is_implicit
                                                      :data_source
                                                      :retain]))
    param))

(defn- format-param
  [{param-type :type
    value-type :value_type
    param-values :parameter_values
    validation-rules :validation_rules
    :as param}]
  (when-not value-type
    (throw+ {:code    cc-errs/ERR_NOT_WRITEABLE
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
                          :data_source
                          :retain)
                  remove-nil-vals)]
    (if (contains? persistence/param-list-types param-type)
      (format-list-param param param-values)
      (assoc param :defaultValue (-> param-values first :value)))))

(defn- format-group
  [group]
  (remove-nil-vals
    (update-in group [:parameters] (partial map format-param))))

(defn- format-app-for-editing
  [app]
  (let [app (get-app-details (:id app))
        task (first (:tasks app))]
    (when (empty? tasks)
      (throw+ {:code    cc-errs/ERR_NOT_WRITEABLE
               :message "App contains no steps and cannot be copied or modified."}))
    (remove-nil-vals
      (-> app
          (assoc :references (map :reference_text (:app_references app))
                 :tools      (remove-nil-vals (persistence/get-app-tools (:id app)))
                 :groups     (map format-group (:parameter_groups task)))
          (dissoc :app_references
                  :tasks)))))

(defn edit-app
  "This service prepares a JSON response for editing an App in the client."
  [app-id]
  (let [app (persistence/get-app app-id)]
    (verify-app-editable app)
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

(defn- add-validation-rule
  "Adds an App parameter's validator and its rule arguments."
  [parameter-id {validator-type :type rule-args :params}]
  (let [validation-rule-id (:id (persistence/add-validation-rule parameter-id validator-type))]
    (dorun (map-indexed (partial persistence/add-validation-rule-argument validation-rule-id)
                        rule-args))))

(defn- update-app-parameter
  "Adds or updates an App parameter and any associated file parameters, validators, and arguments."
  [group-id display-order {param-id :id
                           default-value :defaultValue
                           param-type :type
                           file-parameter :file_parameters
                           validators :validators
                           arguments :arguments
                           :as parameter}]
  (let [update-values (assoc parameter :parameter_group_id group-id :display_order display-order)
        param-exists (and param-id (persistence/get-app-parameter param-id))
        param-id (if param-exists
                   param-id
                   (-> update-values
                       (dissoc :id)
                       (persistence/add-app-parameter)
                       (:id)))
        parameter (assoc parameter :id param-id)]
    (when param-exists
      (persistence/update-app-parameter update-values)
      (persistence/remove-file-parameter param-id)
      (persistence/remove-parameter-validation-rules param-id)
      (persistence/remove-parameter-values param-id)
      (when-not (contains? persistence/param-file-types param-type)
        (persistence/remove-parameter-mappings param-id)))

    (when-not (or (contains? persistence/param-list-types param-type) (empty? default-value))
      (persistence/add-parameter-default-value param-id default-value))

    (dorun (map (partial add-validation-rule param-id) validators))

    (when (contains? persistence/param-file-types param-type)
      (persistence/add-file-parameter (assoc file-parameter :parameter_id param-id)))

    (remove-nil-vals
        (assoc parameter
          :arguments (when (contains? persistence/param-list-types param-type)
                       (update-param-selection-arguments param-type param-id arguments))))))

(defn- update-app-group
  "Adds or updates an App group and its parameters."
  [task-id display-order {group-id :id parameters :parameters :as group}]
  (let [update-values (assoc group :task_id task-id :display_order display-order)
        group-exists (and group-id (persistence/get-app-group group-id))
        group-id (if group-exists group-id (:id (persistence/add-app-group update-values)))]
    (when group-exists
      (persistence/update-app-group update-values))
    (assoc group
      :id group-id
      :parameters (doall (map-indexed (partial update-app-parameter group-id) parameters)))))

(defn- param-arg-id-reducer
  "A function used in a reduce to collect a parameter argument's ID and all of its childrens' IDs by
   recursively calling itself on its child collections."
  [result argument]
  (concat result
    (reduce param-arg-id-reducer [(:id argument)] (:arguments argument))
    (reduce param-arg-id-reducer [] (:groups argument))))

(defn- delete-parameter-argument-orphans
  "Deletes arguments no longer associated with an App parameter."
  [{param-id :id arguments :arguments :as parameter}]
  (let [argument-ids (reduce param-arg-id-reducer [] (:arguments parameter))]
    (when-not (empty? argument-ids)
      (persistence/remove-parameter-value-orphans param-id argument-ids))))

(defn- delete-app-parameter-orphans
  "Deletes parameters no longer associated with an App group."
  [{group-id :id params :parameters}]
  (let [parameter-ids (remove nil? (map :id params))]
    (when-not (empty? parameter-ids)
      (persistence/remove-parameter-orphans group-id parameter-ids)
      (dorun (map delete-parameter-argument-orphans params)))))

(defn- delete-app-orphans
  "Deletes groups and parameters no longer associated with an App."
  [task-id groups]
  (let [group-ids (remove nil? (map :id groups))]
    (when-not (empty? group-ids)
      (persistence/remove-app-group-orphans task-id group-ids)
      (dorun (map delete-app-parameter-orphans groups)))))

(defn update-app
  "This service will update a single-step App, including the information at its top level and the
   tool used by its single task, as long as the App has not been submitted for public use."
  [{app-id :id :keys [references groups] :as app}]
  (verify-app-editable (persistence/get-app app-id))
  (transaction
    (persistence/update-app app)
    (let [tool-id (->> app :tools first :id)
          task-id (->> (get-app-details app-id)
                       :tasks
                       first
                       :id)
          updated-groups (doall (map-indexed (partial update-app-group task-id) groups))]
      (delete-app-orphans task-id updated-groups)
      (when-not (empty? references)
        (persistence/set-app-references app-id references))
      (when-not (nil? tool-id)
        (persistence/set-task-tool task-id tool-id))
      (service/success-response (assoc app :groups updated-groups)))))

(defn add-app-to-user-dev-category
  "Adds an app with the given ID to the current user's apps-under-development category."
  [app-id]
  (let [workspace-category-id (:root_category_id (get-workspace))
        dev-group-id (get-app-subcategory-id workspace-category-id (workspace-dev-app-group-index))]
    (add-app-to-group dev-group-id app-id)))

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
      (edit-app app-id))))

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

(defn- convert-parameter-argument-to-copy
  [{arguments :arguments groups :groups :as parameter-argument}]
  (-> parameter-argument
      (dissoc :id)
      (assoc :arguments (map convert-parameter-argument-to-copy arguments)
             :groups    (map convert-parameter-argument-to-copy groups))
      (remove-nil-vals)))

(defn- convert-app-parameter-to-copy
  [{arguments :arguments :as parameter}]
  (-> parameter
      (dissoc :id)
      (assoc :arguments (map convert-parameter-argument-to-copy arguments))
      (remove-nil-vals)))

(defn- convert-app-group-to-copy
  [{parameters :parameters :as group}]
  (-> group
      (dissoc :id)
      (assoc :parameters (map convert-app-parameter-to-copy parameters))
      (remove-nil-vals)))

(defn- convert-app-to-copy
  "Removes ID fields from a client formatted App, its groups, parameters, and parameter arguments,
   and formats appropriate app fields to prepare it for saving as a copy."
  [app]
  (let [app (format-app-for-editing app)]
    (-> app
        (dissoc :id)
        (assoc :name   (app-copy-name (:name app))
               :groups (map convert-app-group-to-copy (:groups app)))
        (remove-nil-vals))))

(defn copy-app
  "This service makes a copy of an App available in Tito for editing."
  [app-id]
  (-> app-id
      (persistence/get-app)
      (convert-app-to-copy)
      (add-app)))
