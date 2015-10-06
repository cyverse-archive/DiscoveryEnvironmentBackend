(ns facepalm.c180-2013022501
  (:use [korma.core])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130225.01")

(defn- old-style-selection-properties
  "Lists properties describing selection lists in their original format."
  []
  (select :property
          (modifier "distinct")
          (fields :property.hid :property.name)
          (join :validator {:property.validator :validator.hid})
          (join :validator_rule {:validator.hid :validator_rule.validator_id})
          (join :rule {:validator_rule.rule_id :rule.hid})
          (join :rule_type {:rule.rule_type :rule_type.hid})
          (join :rule_argument {:rule.hid :rule_argument.rule_id})
          (where (and (= :rule_type.name "MustContain")
                      (not (like :rule_argument.argument_value "{%}"))))))

(defn- load-selection-labels
  "Loads the selection labels from an old-style MustContain rule"
  [property-hid]
  (select :property
          (fields [:rule.hid :rule_hid] [:rule_argument.argument_value :value] :rule_argument.hid)
          (join :validator {:property.validator :validator.hid})
          (join :validator_rule {:validator.hid :validator_rule.validator_id})
          (join :rule {:validator_rule.rule_id :rule.hid})
          (join :rule_type {:rule.rule_type :rule_type.hid})
          (join :rule_argument {:rule.hid :rule_argument.rule_id})
          (where {:property.hid   property-hid
                  :rule_type.name "MustContain"})))

(defn- convert-must-contain-arg
  "Converts one argument in an old-style MustContain rule to the new style."
  [rule-hid arg-hid option value display]
  (let [arg-value (cheshire/encode {:name      option
                                    :value     (or value "")
                                    :display   display
                                    :isDefault false})]
    (update :rule_argument
            (set-fields {:argument_value arg-value})
            (where {:rule_id rule-hid
                    :hid     arg-hid}))))

(defn- convert-must-contain-rule
  "Converts a single old-style MustContain rule to the new style."
  [{hid :hid arg-str :name}]
  (let [args         (string/split arg-str #",\s*")
        labels       (load-selection-labels hid)
        split-arg    #(string/split (string/trim %) #"=|\s+" 2)]
    (if (= 1 (count args))
      (dorun (map (fn [{rule-hid :rule_hid arg-hid :hid value :value}]
                    (convert-must-contain-arg rule-hid arg-hid arg-str value value))
                  labels))
      (dorun (map (fn [[opt value] {rule-hid :rule_hid arg-hid :hid display :value}]
                    (convert-must-contain-arg rule-hid arg-hid opt value display))
                  (map split-arg args)
                  labels)))))

(defn- convert-must-contain-rules
  "Converts the old-style MustContain rules to the new style."
  []
  (println "\t* converting MustContain rules")
  (dorun (map convert-must-contain-rule (old-style-selection-properties))))

(defn convert
  "Performs the database conversion for DE version 1.8.0:20130225.01."
  []
  (println "Performing conversion for" version)
  (convert-must-contain-rules))
