(ns facepalm.c180-2013062701
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130627.01")

(defn- prop-type-subselect
  "Creates a subselect statement for a property type with the given name."
  [type-name]
  (subselect :property_type
             (fields :hid)
             (where {:name type-name})))

(defn- convert-selection-props
  "Converts 'Selection' properties to 'TextSelection' properties."
  []
  (println "\t* converting 'Selection' properties to 'TextSelection'.")
  (update :property
          (set-fields {:property_type (prop-type-subselect "TextSelection")})
          (where {:property_type (prop-type-subselect "Selection")})))

(defn- rule-argument-subselect
  "Creates a subselect statement that can be used to determine whether or not a property
   has any rule arguments associated with it."
  []
  (subselect [:validator :v]
               (join [:validator_rule :vr] {:v.hid :vr.validator_id})
               (join [:rule :r] {:vr.rule_id :r.hid})
               (join [:rule_argument :ra] {:r.hid :ra.rule_id})
               (where {:property.validator :v.hid})))

(defn- selection-value-subselect
  "Creates a subselect statement that can be used to determine if a 'Selection' or
   'ValueSelection' property is not associated with any values that match one of the
   given patterns. The property in the statement containing this subselect should not
   have an alias."
  [patterns]
  (letfn [(value-does-not-match [s]
            [(raw (str "ra.argument_value !~* '.*\"display\":\"" s "\".*'"))
             (raw (str "ra.argument_value !~* '.*\"display\":" s ",.*'"))
             (raw (str "ra.argument_value !~* '.*\"display\":" s "}.*'"))])]
    (subselect [:validator :v]
               (join [:validator_rule :vr] {:v.hid :vr.validator_id})
               (join [:rule :r] {:vr.rule_id :r.hid})
               (join [:rule_argument :ra] {:r.hid :ra.rule_id})
               (where (apply and
                             {:property.validator :v.hid}
                             (mapcat value-does-not-match patterns))))))

(defn- convert-value-selection-props
  "Converts 'ValueSelection' properties that contain only values matching the given patterns to
   another type of property."
  [new-property-type patterns]
  (update :property
          (set-fields {:property_type (prop-type-subselect new-property-type)})
          (where (and {:property_type (prop-type-subselect "ValueSelection")}
                      (sqlfn "EXISTS " (rule-argument-subselect))
                      (sqlfn "NOT EXISTS " (selection-value-subselect patterns))))))

(defn- convert-double-selection-props
  "Converts 'ValueSelection' properties that contain only values that appear to be floating point
   numbers to 'DoubleSelection' properties."
  []
  (println "\t* converting some 'ValueSelection' properties to 'DoubleSelection'.")
  (convert-value-selection-props
   "DoubleSelection"
   ["[-+]?\\d*\\.\\d+"
    "[-+]?\\d+\\."
    "[-+]?\\d+e\\d+"
    "[-+]?\\d*\\.\\d+e\\d+"
    "[-+]?\\d+\\.e\\d+"]))

(defn- convert-integer-selection-props
  "Converts 'ValueSelection' properties that contain only values that appear to be integers
   to 'IntegerSelection' properties."
  []
  (println "\t* converting some 'ValueSelection' properties to 'IntegerSelection'.")
  (convert-value-selection-props
   "IntegerSelection"
   ["[-+]?\\d+"]))

(defn- convert-text-selection-props
  "Converts the remaining 'ValueSelection' properties to 'TextSelection' properties."
  []
  (println "\t* converting remaining 'ValueSelection' properties to 'TextSelection'.")
  (update :property
          (set-fields {:property_type (prop-type-subselect "TextSelection")})
          (where {:property_type (prop-type-subselect "ValueSelection")})))

(defn convert
  "Performs the conversion for database version 1.8.0:20130627.01."
  []
  (println "Performing conversion for" version)
  (convert-selection-props)
  (convert-double-selection-props)
  (convert-integer-selection-props)
  (convert-text-selection-props))
