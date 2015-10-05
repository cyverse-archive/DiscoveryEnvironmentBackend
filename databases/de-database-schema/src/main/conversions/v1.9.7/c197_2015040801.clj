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

(defn- add-metadata-template-created-modified-columns
  "CORE-6635: Add created/modified columns to metadata_templates and metadata_attributes tables.
   The created_by and modified_by user IDs are initially set to the <public> user's ID."
  []
  (println "\t* adding metadata_templates table created/modified columns")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD COLUMN created_by uuid
     DEFAULT '00000000-0000-0000-0000-000000000000'
     NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ALTER COLUMN created_by DROP DEFAULT")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD CONSTRAINT metadata_templates_created_by_fkey
     FOREIGN KEY (created_by)
     REFERENCES users(id)")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD COLUMN modified_by uuid
     DEFAULT '00000000-0000-0000-0000-000000000000'
     NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ALTER COLUMN modified_by DROP DEFAULT")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD CONSTRAINT metadata_templates_modified_by_fkey
     FOREIGN KEY (modified_by)
     REFERENCES users(id)")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD COLUMN created_on timestamp
     DEFAULT now() NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_templates
     ADD COLUMN modified_on timestamp
     DEFAULT now() NOT NULL")

  (println "\t* adding metadata_attributes table created/modified columns")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD COLUMN created_by uuid
     DEFAULT '00000000-0000-0000-0000-000000000000'
     NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ALTER COLUMN created_by DROP DEFAULT")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD CONSTRAINT metadata_attributes_created_by_fkey
     FOREIGN KEY (created_by)
     REFERENCES users(id)")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD COLUMN modified_by uuid
     DEFAULT '00000000-0000-0000-0000-000000000000'
     NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ALTER COLUMN modified_by DROP DEFAULT")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD CONSTRAINT metadata_attributes_modified_by_fkey
     FOREIGN KEY (modified_by)
     REFERENCES users(id)")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD COLUMN created_on timestamp
     DEFAULT now() NOT NULL")
  (exec-sql-statement
    "ALTER TABLE ONLY metadata_attributes
     ADD COLUMN modified_on timestamp
     DEFAULT now() NOT NULL"))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (update-metadata-attr-synonyms-constraints)
  (add-metadata-template-created-modified-columns))
