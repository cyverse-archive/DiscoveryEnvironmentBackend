(ns facepalm.c200-2015060101
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]])
  (:require [clojure.java.io :as io]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150601.01")

(defn- construct-path
  [& components]
  (.getPath (apply io/file components)))

(def ^:private metadata-template-table-files
  (->> ["attr_enum_values.sql"
        "attr_synonyms.sql"
        "attributes.sql"
        "template_attrs.sql"
        "templates.sql"
        "value_types.sql"]
       (map (partial construct-path "tables"))
       (doall)))

(defn- add-metadata-template-tables
  []
  (println "\t* adding the metadata template tables")
  (dorun (map load-sql-file metadata-template-table-files)))

(def ^:private metadata-template-table-constraint-files
  (->> ["03_value_types.sql"
        "04_templates.sql"
        "05_attributes.sql"
        "06_template_attrs.sql"
        "07_attr_enum_values.sql"
        "08_attr_synonyms.sql"]
       (map (partial construct-path "constraints"))
       (doall)))

(defn- add-metadata-template-table-constraints
  []
  (println "\t* adding the metadata template table constraints")
  (dorun (map load-sql-file metadata-template-table-constraint-files)))

(defn- add-metadata-attribute-synonyms-function
  []
  (println "\t* adding the metadata attribute synonyms function")
  (load-sql-file "functions/01_attribute_synonyms.sql"))

(defn- print-warning
  [& lines]
  (println)
  (dorun (map (partial println "\t ") lines)))

(defn- print-conversion-warning
  []
  (print-warning
   "WARNING: this conversion requires data to be copied from the DE database to the"
   "metadata database. Please ensure that the conversion utility that copies this data"
   "is executed after this conversion is done."))

(defn convert
  []
  (println "Performing the conversion for" version)
  (add-metadata-template-tables)
  (add-metadata-template-table-constraints)
  (add-metadata-attribute-synonyms-function)
  (print-conversion-warning))
