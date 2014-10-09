(ns metadactyl.zoidberg.app-edit
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.app-groups :only [add-app-to-group get-app-subcategory-id]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [metadactyl.persistence.app-metadata :only [add-app get-app update-app]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config :only [workspace-dev-app-group-index]]
        [metadactyl.util.conversions :only [date->long remove-nil-vals convert-rule-argument]]
        [metadactyl.validation :only [verify-app-editable verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]])
  (:require [metadactyl.util.service :as service]))

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
             (join :tools {:tools.id :tasks.tool_id})
             (fields :id
                     :tasks.tool_id
                     [:tools.name :tool])
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
                 (with parameter_types)
                 (with validation_rules
                   (with rule_type)
                   (with validation_rule_arguments
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
  (if (= param-type "TreeSelection")
    [(format-tree-params param-values)]
    (map format-param-value param-values)))

(defn- format-list-param
  [param param-values]
  (let [param-type (:type param)
        param-args (format-list-type-params param-type param-values)
        param (if-not (= param-type "TreeSelection")
                (assoc param :defaultValue (first (filter :isDefault param-args)))
                param)]
    (assoc param :arguments param-args)))

(defn- format-param
  [param]
  (let [param-type (:type param)
        param-values (:parameter_values param)
        param (-> param
                  (assoc :validators (map format-validator (:validation_rules param)))
                  (dissoc :parameter_values
                          :validation_rules)
                  remove-nil-vals)]
    (if (contains? #{"TextSelection"
                     "IntegerSelection"
                     "DoubleSelection"
                     "TreeSelection"}
                   param-type)
      (format-list-param param param-values)
      (assoc param :defaultValue (-> param-values first :value)))))

(defn- format-group
  [group]
  (remove-nil-vals
    (update-in group [:parameters] (partial map format-param))))

(defn- format-app
  [app]
  (let [app (get-app-details (:id app))
        task (first (:tasks app))
        groups (map format-group (:parameter_groups task))]
    (remove-nil-vals
      (-> app
          (assoc :integration_date (date->long (:integration_date app))
                 :edited_date (date->long (:edited_date app))
                 :references (map :reference_text (:app_references app))
                 :tool (:tool task)
                 :tool_id (:tool_id task)
                 :groups groups)
          (dissoc :app_references
                  :tasks)))))

(defn edit-app
  "This service prepares a JSON response for editing an App in the client."
  [app-id]
  (let [app (get-app app-id)]
    (verify-app-editable app)
    (service/swagger-response (format-app app))))

;; FIXME
(defn copy-app
  "This service makes a copy of an App available in Tito for editing."
  [app-id]
  (let [app (get-app app-id)
        ;;app (convert-app-to-copy app)
        ;;app-id (add-pipeline-app app)
        ]
    (edit-app app-id)))
