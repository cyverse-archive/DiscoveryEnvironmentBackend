(ns facepalm.c180-2013061301
  (:use [korma.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130613.01")

(defn- property-type-subselect
  [type]
  (subselect :property_type
             (fields :hid)
             (where {:name type})))

(defn- rule-type-subselect
  [type-name-pattern]
  (subselect [:validator :v]
             (join [:validator_rule :vr] {:v.hid :vr.validator_id})
             (join [:rule :r] {:vr.rule_id :r.hid})
             (join [:rule_type :rt] {:r.rule_type :rt.hid})
             (where {:p.validator :v.hid
                     :rt.name     [like type-name-pattern]})))

(defn- double-property-subselect
  []
  (subselect [:property :p]
             (fields :p.hid)
             (join [:property_type :pt] {:p.property_type :pt.hid})
             (where (and {:pt.name "Number"}
                         (sqlfn "NOT EXISTS " (rule-type-subselect "Int%"))
                         (or (sqlfn "EXISTS " (rule-type-subselect "Double%"))
                             {:p.defalut_value nil}
                             {:p.defalut_value ""}
                             {:p.defalut_value [like "%.%"]}
                             {:p.defalut_value [like "%e%"]}
                             {:p.defalut_value [like "%E%"]})))))

(defn- number-property-subselect
  []
  (subselect [:property :p]
             (fields :p.hid)
             (join [:property_type :pt] {:p.property_type :pt.hid})
             (where {:pt.name "Number"})))

(defn- convert-number-to-double
  []
  (println "\t* converting some Number properties to Double properties.")
  (update :property
          (set-fields {:property_type (property-type-subselect "Double")})
          (where {:hid [in (double-property-subselect)]})))

(defn- convert-number-to-integer
  []
  (println "\t* converting remaining Number proeprties to Integer Properties.")
  (update :property
          (set-fields {:property_type (property-type-subselect "Integer")})
          (where {:hid [in (number-property-subselect)]})))

(defn convert
  "Performs the conversion for database version 1.8.0:20130613.01."
  []
  (println "Performing conversion for" version)
  (convert-number-to-double)
  (convert-number-to-integer))
