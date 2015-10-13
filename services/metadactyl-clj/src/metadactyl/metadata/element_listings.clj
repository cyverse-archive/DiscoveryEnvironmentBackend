(ns metadactyl.metadata.element-listings
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.queries]
        [korma.core :exclude [update]]
        [metadactyl.tools :only [tool-listing-base-query]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [throw+]]))

(defn- base-parameter-type-query
  "Creates the base query used to list parameter types for the metadata element listing service."
  []
  (-> (select* parameter_types)
      (fields :parameter_types.id :parameter_types.name
              [:value_type.name :value_type] :parameter_types.description)
      (join value_type)
      (where {:deprecated false})
      (order :display_order)))

(defn- get-tool-type-id
  "Gets the internal identifier associated with a tool type name."
  [tool-type-name]
  (let [result (get-tool-type-by-name tool-type-name)]
    (when (nil? result)
      (throw+ {:type ::unknown_tool_type
               :name tool-type-name}))
    (:id result)))

(defn- get-tool-type-for-tool-id
  "Gets the tool type associated with the given tool identifier."
  [tool-id]
  (let [result (get-tool-type-by-component-id tool-id)]
    (when (nil? result)
      (throw+ {:type ::unknown_tool
               :id   tool-id}))
    (:id result)))

(defn- get-tool-type
  "Gets the tool type to use when listing property types.  If the tool type is
   specified directly then we'll use that in the query.  If the deployed
   component is specified then its associated tool type will be used in the
   query.  Otherwise, all property types will be listed."
  [tool-type tool-id]
  (cond (not (nil? tool-type)) (get-tool-type-id tool-type)
        (not (nil? tool-id))   (get-tool-type-for-tool-id tool-id)
        :else                  nil))

(defn- list-data-formats
  "Obtains a listing of data formats known to the DE."
  [_]
  {:formats
   (map remove-nil-vals
     (select data_formats
             (fields :id :name :label)
             (order :display_order)))})

(defn- list-data-sources
  "Obtains a listing of data sources."
  [_]
  {:data_sources
   (select data_source
           (fields :id :name :label :description)
           (order :display_order))})

(defn- list-tools
  "Obtains a listing of tools for the metadata element listing service."
  [params]
  {:tools (->> (select-keys params [:include-hidden])
               (tool-listing-base-query)
               (select)
               (map remove-nil-vals))})

(defn- list-info-types
  "Obtains a listing of information types for the metadata element listing service."
  [_]
  {:info_types
   (map remove-nil-vals
     (select info_type
             (fields :id :name :label)
             (where {:deprecated false})
             (order :display_order)))})

(defn- list-property-types
  "Obtains the property types for the metadata element listing service.
   Parameter types may be filtered by tool type or tool.  If the tool type is specified only
   parameter types that are associated with that tool type will be listed.  If the tool is specified
   then only parameter tpes associated with the tool type that is associated with the tool will be
   listed.  Specifying an invalid tool type name or tool id will result in an error."
  [{:keys [tool-type tool-id]}]
  (let [tool-type-id (get-tool-type tool-type tool-id)]
    {:parameter_types
     (map remove-nil-vals
       (if (nil? tool-type-id)
         (select (base-parameter-type-query))
         (parameter-types-for-tool-type (base-parameter-type-query) tool-type-id)))}))

(defn- list-rule-types
  "Obtains the list of rule types for the metadata element listing service."
  [_]
  {:rule_types
   (mapv
    (fn [m]
      (remove-nil-vals
        (assoc (dissoc m :value_type)
          :value_types             (mapv :name (:value_type m))
          :rule_description_format (:rule_description_format m ""))))
    (select rule_type
            (fields [:rule_type.id :id]
                    [:rule_type.name :name]
                    [:rule_type.description :description]
                    [:rule_subtype.name :subtype]
                    [:rule_type.rule_description_format :rule_description_format])
            (join rule_subtype)
            (with value_type)))})

(defn- list-tool-types
  "Obtains the list of tool types for the metadata element listing service."
  [_]
  {:tool_types (map remove-nil-vals (select tool_types (fields :id :name :label :description)))})

(defn- list-value-types
  "Obtains the list of value types for the metadata element listing service."
  [_]
  {:value_types
   (select value_type
           (fields :id :name :description))})

(def ^:private listing-fns
  "The listing functions to use for various metadata element types."
  {"data-sources"    list-data-sources
   "file-formats"    list-data-formats
   "info-types"      list-info-types
   "parameter-types" list-property-types
   "rule-types"      list-rule-types
   "tools"           list-tools
   "tool-types"      list-tool-types
   "value-types"     list-value-types})

(defn- list-all
  "Lists all of the element types that are available to the listing service."
  [params]
  (reduce merge {} (map #(% params) (vals listing-fns))))

  (defn list-elements "Lists selected workflow elements.  This function handles requests to list
   various different types of workflow elements."
  [elm-type params]
    (cond
      (= elm-type "all")               (list-all params)
      (contains? listing-fns elm-type) ((listing-fns elm-type) params)
      :else (throw+ {:type ::unrecognized_workflow_component_type
                     :name elm-type})))
