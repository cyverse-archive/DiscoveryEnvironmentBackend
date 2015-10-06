(ns facepalm.c184-2014011401
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.4:20140114.01")

(defn- str->uuid
  "Converts a string representation of a UUID to a UUID class."
  [s]
  (UUID/fromString s))

(defn- add-metadata-template-attrs-table
  []
  (println "\t* adding the metadata_template_attrs table")
  (exec-raw
   "CREATE TABLE metadata_template_attrs (
    template_id uuid NOT NULL REFERENCES metadata_templates(id),
    attribute_id uuid NOT NULL REFERENCES metadata_attributes(id),
    display_order integer NOT NULL)")
  (exec-raw
   "CREATE INDEX metadata_template_attrs_template_id
    ON metadata_template_attrs(template_id)")
  (exec-raw
   "CREATE INDEX metadata_template_attrs_attribute_id
    ON metadata_template_attrs(attribute_id)"))

(defn- populate-metadata-template-attrs-table
  []
  (println "\t* populating the metadata_template_attrs table")
  (exec-raw
   "INSERT INTO metadata_template_attrs(template_id, attribute_id, display_order)
    SELECT template_id, id, display_order FROM metadata_attributes"))

(defn- drop-unused-columns
  []
  (println "\t* removing unused columns from the metadata_attributes table")
  (exec-raw
   "ALTER TABLE metadata_attributes
    DROP COLUMN template_id")
  (exec-raw
   "ALTER TABLE metadata_attributes
    DROP COLUMN display_order"))

(defn- add-metadata-attr-synonyms-table
  []
  (println "\t* adding the metadata_attr_synonyms table")
  (exec-raw
   "CREATE TABLE metadata_attr_synonyms (
    attribute_id uuid NOT NULL REFERENCES metadata_attributes(id),
    synonym_id uuid NOT NULL REFERENCES metadata_attributes(id))")
  (exec-raw
   "CREATE INDEX metadata_attr_synonyms_attribute_id
    ON metadata_attr_synonyms(attribute_id)")
  (exec-raw
   "CREATE INDEX metadata_attr_synonyms_synonym_id
    ON metadata_attr_synonyms(synonym_id)"))

(defn convert
  "Performs the conversion for database version 1.8.4:20140114.01."
  []
  (println "Performing conversion for" version)
  (add-metadata-template-attrs-table)
  (populate-metadata-template-attrs-table)
  (drop-unused-columns)
  (add-metadata-attr-synonyms-table))
