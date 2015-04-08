(ns facepalm.c197-2015040801
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.7:20150408.01")

(defn- update-metadata-attr-synonyms-constraints
  "CORE-6635: Update metadata_attr_synonyms so synonyms are deleted when attributes are deleted."
  []
  (println "\t* dropping metadata_attr_synonyms table constraints")
  (exec-sql-statement "ALTER TABLE ONLY metadata_attr_synonyms DROP CONSTRAINT metadata_attr_synonyms_attribute_id_fkey")
  (exec-sql-statement "ALTER TABLE ONLY metadata_attr_synonyms DROP CONSTRAINT metadata_attr_synonyms_synonym_id_fkey")
  (println "\t* re-adding metadata_attr_synonyms table constraints")
  (load-sql-file "constraints/62_metadata_attr_synonyms.sql"))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (update-metadata-attr-synonyms-constraints))
