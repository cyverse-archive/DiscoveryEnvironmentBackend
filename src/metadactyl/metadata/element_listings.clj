(ns metadactyl.metadata.element-listings
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.queries]
        [korma.core]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]))

(defn- base-property-type-query
  "Creates the base query used to list property types for the metadata element
   listing service."
  []
  (-> (select* property_type)
      (fields :property_type.hid :property_type.id :property_type.name
              [:value_type.name :value_type] :property_type.description)
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

(defn- get-tool-type-for-component-id
  "Gets the tool type associated with the given deployed component identifier."
  [component-id]
  (let [result (get-tool-type-by-component-id component-id)]
    (when (nil? result)
      (throw+ {:type ::unknown_deployed_component
               :id   component-id}))
    (:id result)))

(defn- get-tool-type
  "Gets the tool type to use when listing property types.  If the tool type is
   specified directly then we'll use that in the query.  If the deployed
   component is specified then its associated tool type will be used in the
   query.  Otherwise, all property types will be listed."
  [tool-type component-id]
  (cond (not (nil? tool-type))    (get-tool-type-id tool-type)
        (not (nil? component-id)) (get-tool-type-for-component-id component-id)
        :else                     nil))

(defn- list-data-formats
  "Obtains a listing of data formats known to the DE."
  [_]
  {:formats
   (select data_formats
           (fields [:id :hid] [:guid :id] :name :label)
           (order :display_order))})

(defn- list-data-sources
  "Obtains a listing of data sources."
  [_]
  {:data_sources
   (select data_source
           (fields [:id :hid] [:uuid :id] :name :label)
           (order :id))})

(defn- list-deployed-components
  "Obtains a listing of deployed components for the metadata element listing service."
  [_]
  {:components
   (select deployed_components
           (fields [:deployed_components.id :id]
                   [:deployed_components.name :name]
                   [:deployed_components.description :description]
                   [:deployed_components.hid :hid]
                   [:deployed_components.location :location]
                   [:tool_types.name :type]
                   [:deployed_components.version :version]
                   [:deployed_components.attribution :attribution])
           (join tool_types))})

(defn- list-info-types
  "Obtains a listing of information types for the metadata element listing service."
  [_]
  {:info_types
   (select info_type
           (fields :id :name :label :description :hid)
           (where {:deprecated false})
           (order :display_order))})

(defn- list-property-types
  "Obtains the property types for the metadata element listing service.
   Property types may be filtered by tool type or deployed component.  If the
   tool type is specified only property types that are associated with that
   tool type will be listed.  If the deployed component is specified then only
   property tpes associated with the tool type that is associated with the
   deployed component will be listed.  Specifying an invalid tool type name or
   deployed component id will result in an error."
  [{:keys [tool-type component-id]}]
  (let [tool-type-id (get-tool-type tool-type component-id)]
    {:property_types
     (if (nil? tool-type-id)
       (select (base-property-type-query))
       (property-types-for-tool-type (base-property-type-query) tool-type-id))}))

(defn- list-rule-types
  "Obtains the list of rule types for the metadata element listing service."
  [_]
  {:rule_types
   (mapv
    (fn [m]
      (assoc (dissoc m :value_type)
        :value_types             (mapv :name (:value_type m))
        :rule_description_format (:rule_description_format m "")))
    (select rule_type
            (fields [:rule_type.id :id]
                    [:rule_type.name :name]
                    [:rule_type.label :label]
                    [:rule_type.description :description]
                    [:rule_type.hid :hid]
                    [:rule_subtype.name :subtype]
                    [:rule_type.rule_description_format :rule_description_format])
            (join rule_subtype)
            (with value_type)))})

(defn- list-tool-types
  "Obtains the list of tool types for the metadata element listing service."
  [_]
  {:tool_types (select tool_types)})

(defn- list-value-types
  "Obtains the list of value types for the metadata element listing service."
  [_]
  {:value_types
   (select value_type
           (fields :hid :id :name :description))})

(def ^:private listing-fns
  "The listing functions to use for various metadata element types."
  {"components"     list-deployed-components
   "data-sources"   list-data-sources
   "formats"        list-data-formats
   "info-types"     list-info-types
   "property-types" list-property-types
   "rule-types"     list-rule-types
   "tool-types"     list-tool-types
   "value-types"    list-value-types})

(defn- list-all
  "Lists all of the element types that are available to the listing service."
  [params]
  (reduce merge {} (map #(% params) (vals listing-fns))))

(defn list-elements
  "Lists selected workflow elements.  This function handles requests to list
   various different types of workflow elements."
  [elm-type params]
  (cond
   (= elm-type "all")               (list-all params)
   (contains? listing-fns elm-type) ((listing-fns elm-type) params)
   :else                            (throw+ {:type ::unrecognized_workflow_component_type
                                             :name elm-type})))
