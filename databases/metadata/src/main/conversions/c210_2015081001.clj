(ns facepalm.c210-2015081001
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150810.01")

(defn convert
  []
  (println "Performing the conversion for" version ", adding a unique constraint on value_type.name")
  (exec-raw "ALTER TABLE value_types
    ADD CONSTRAINT value_types_unique_name
    UNIQUE (name);"))
