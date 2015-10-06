(ns facepalm.c182-2013092401
  (:use [korma.core])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.8.2:20130924.01")

(def ^:private hidable-property-types
  "The property types that should be marked as hidable."
  ["Text"
   "Flag"
   "MultiLineText"
   "Output"
   "EnvironmentVariable"
   "Integer"
   "Double"
   "FileOutput"
   "FolderOutput"
   "MultiFileOutput"])

(defn- add-hidable-flag-to-property-types
  "Some property types should not be marked as hidden. Add a column to the property_type table
   to indicate whether or not properties of that type can be marked as hidden, and initialize
   the existing property types with the appropriate values for this flag."
  []
  (println "\t* adding the hidable column to the property_type table")
  (exec-raw "ALTER TABLE property_type ADD COLUMN hidable boolean DEFAULT FALSE")
  (update :property_type
          (set-fields {:hidable true})
          (where {:name [in hidable-property-types]})))

(defn- parse-json
  "Attempts to parse an encoded JSON string, returning nil if parsing fails."
  [json]
  (when-not (nil? json)
    (try
      (cheshire/decode json true)
      (catch Exception _ nil))))

(defn- get-selections-for-validator
  "Gets the selection list element values for a validator."
  [validator-hid]
  (select [:validator :v]
          (join [:validator_rule :vr]
                {:v.hid :vr.validator_id})
          (join [:rule :r]
                {:vr.rule_id :r.hid})
          (join [:rule_argument :ra]
                {:r.hid :ra.rule_id})
          (join [:rule_type :rt]
                {:r.rule_type :rt.hid})
          (order :ra.hid)
          (fields :ra.argument_value)
          (where {:v.hid   validator-hid
                  :rt.name "MustContain"})))

(defn- get-default-selection-for-validator
  "Gets the default slelection list element value for a validator."
  [validator-hid]
  (->> (get-selections-for-validator validator-hid)
       (map (comp parse-json :argument_value))
       (remove nil?)
       (filter map?)
       (filter :isDefault)
       (map (juxt :name :value))
       (first)))

(defn- determine-default-value
  "Determines the default value for the property associated with a validator."
  [validator-hid]
  (or (get-default-selection-for-validator validator-hid)
      ["" ""]))

(defn- fix-default-value
  "Fixes the default value of a property with a null default value."
  [{:keys [hid validator]}]
  (let [[n v] (determine-default-value validator)
        vals  (if-not (string/blank? n)
                {:name          n
                 :defalut_value v}
                {:defalut_value v})]
    (update :property
            (set-fields vals)
            (where {:hid hid}))))

(defn- fix-hidden-props
  "Some existing properties in the database are hidden and have a null default value. This
   can cause problems when jobs are submitted for these apps. In most cases, the default
   values can simply be replaced with an empty string. There are some cases where the property
   has a MustContain rule, however. In those cases, we can determine the correct default
   values from the rule arguments."
  []
  (println "\t* ensuring that no hidden properties have null default values")
  (->> (select :property
               (fields :hid :validator)
               (where {:is_visible    false
                       :defalut_value nil}))
       (map fix-default-value)
       (dorun)))

(defn- update-property-label-type
  "Updates the property table's label column to a text type."
  []
  (println "\t* updating property label field to TYPE text")
  (exec-raw "ALTER TABLE property ALTER COLUMN label TYPE text"))

(defn convert
  "Performs the conversion for database version 1.8.2:20130924.01."
  []
  (println "Performing conversion for" version)
  (add-hidable-flag-to-property-types)
  (update-property-label-type)
  (fix-hidden-props))
