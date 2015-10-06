(ns facepalm.c144-2012092701
  (:use [korma.core]
        [kameleon.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.4.4:20120927.01")

(defn- alter-rule-argument-value-type
  "Alters the argument_value column in the rule_argument table to a text type."
  []
  (println "\t* altering argument_value column in the rule_argument table")
  (exec-raw
    "ALTER TABLE ONLY rule_argument ALTER COLUMN argument_value TYPE text "))

(defn- get-tool-type-id-subselect
  [job_type_name]
  ""
  (subselect
    :tool_types
    (fields [:tool_types.id :tool_type_id])
    (where {:tool_types.name job_type_name})))

(defn- add-tree-selection-property-type
  "Adds the property type to support the hierarchical list selector."
  []
  (println "\t* adding property type for hierarchical list selector")
  (let [tree_selection_type_id 20]
    (insert :property_type
            (values {:hid           tree_selection_type_id
                     :id            "548A55C2-53FE-40A5-AD38-033F79C8C0AB"
                     :name          "TreeSelection"
                     :description   "A hierarchical list for selecting a choice"
                     :label         nil
                     :deprecated    false
                     :display_order 10
                     :value_type_id 1}))
    (insert :tool_type_property_type
            (values {:tool_type_id (get-tool-type-id-subselect "executable")
                     :property_type_id tree_selection_type_id}))
    (insert :tool_type_property_type
            (values {:tool_type_id (get-tool-type-id-subselect "fAPI")
                     :property_type_id tree_selection_type_id}))))

(defn convert
  "Performs the conversions for database version 1.4.4:20120927.01."
  []
  (println "Performing conversion for" version)
  (alter-rule-argument-value-type)
  (add-tree-selection-property-type))
