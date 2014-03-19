(ns facepalm.c140-2012052501
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120525.01")

(defn- add-edited-date
  "Adds the edited date column to the transformation_activity table."
  []
  (println "\t* adding edited_date to transformation_activity")
  (exec-raw (str "ALTER TABLE transformation_activity "
                 "ADD COLUMN edited_date TIMESTAMP")))

(defn- redefine-analysis-listing
  "Drops the analysis_listing view and redefines it with the edited date and
   overall job type."
  []
  (println "\t* adding edited_date and overall_job_type to analysis_listing")
  (exec-raw "DROP VIEW analysis_listing")
  (exec-raw
   "CREATE VIEW analysis_listing AS
    SELECT analysis.hid,
           analysis.id,
           analysis.name,
           analysis.description,
           integration.integrator_name,
           integration.integrator_email,
           analysis.integration_date,
           analysis.edited_date,
           analysis.wikiurl,
           CAST(COALESCE(AVG(ratings.rating), 0.0) AS DOUBLE PRECISION)
               AS average_rating,
           EXISTS (
               SELECT *
               FROM template_group_template tgt
               JOIN template_group tg ON tgt.template_group_id = tg.hid
               JOIN workspace w ON tg.workspace_id = w.id
               WHERE analysis.hid = tgt.template_id
               AND w.is_public IS TRUE
           ) AS is_public, (
               SELECT COUNT(*)
               FROM transformation_task_steps tts
               WHERE tts.transformation_task_id = analysis.hid
           ) AS step_count,
           analysis.deleted,
           analysis.disabled,
           CASE WHEN COUNT(DISTINCT dc.type) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT dc.type) > 1 THEN 'mixed'
                ELSE MAX(dc.type)
           END AS overall_job_type
    FROM transformation_activity analysis
         LEFT JOIN integration_data integration
             ON analysis.integration_data_id = integration.id
         LEFT JOIN ratings ON analysis.hid = ratings.transformation_activity_id
         LEFT JOIN transformation_task_steps tts
             ON analysis.hid = tts.transformation_task_id
         LEFT JOIN transformation_steps ts
             ON tts.transformation_step_id = ts.id
         LEFT JOIN transformations tx ON ts.transformation_id = tx.id
         LEFT JOIN template t ON tx.template_id = t.id
         LEFT JOIN deployed_components dc ON t.component_id = dc.id
    GROUP BY analysis.hid,
             analysis.id,
             analysis.name,
             analysis.description,
             integration.integrator_name,
             integration.integrator_email,
             analysis.integration_date,
             analysis.edited_date,
             analysis.wikiurl,
             analysis.deleted,
             analysis.disabled"))

(defn- create-analysis-job-types
  "Creates the analysis_job_types view, which lists all of the job types
   associated with an analysis."
  []
  (println "\t* defining analysis_job_types view")
  (exec-raw
   "CREATE VIEW analysis_job_types AS
    SELECT a.hid AS analysis_id,
           dc.type AS job_type
    FROM transformation_activity a
        JOIN transformation_task_steps tts
             ON a.hid = tts.transformation_task_id
        JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
        JOIN transformations tx ON ts.transformation_id = tx.id
        JOIN template t ON tx.template_id::text = t.id::text
        JOIN deployed_components dc ON t.component_id::text = dc.id::text"))

(defn- redefine-version-table
  "Redefines the version table."
  []
  (println "\t* redefining version table")
  (exec-raw "DROP TABLE version")
  (exec-raw "CREATE SEQUENCE version_id_seq")
  (exec-raw
   "CREATE TABLE version (
        id bigint DEFAULT nextval('version_id_seq'),
        version character varying(20) NOT NULL,
        applied timestamp DEFAULT now(),
        PRIMARY KEY (id)
    )"))

(defn- drop-obsolete-tables
  "Drops obsolete tables from the database."
  []
  (println "\t* dropping obsolete tables")
  (exec-raw "DROP TABLE block_type")
  (exec-raw "DROP TABLE file_type")
  (exec-raw "ALTER TABLE workspace DROP CONSTRAINT workspace_home_folder_fkey")
  (exec-raw "DROP TABLE folder_folder")
  (exec-raw "DROP TABLE folder")
  (exec-raw "DROP TABLE IF EXISTS precedence_constraints")
  (exec-raw "DROP SEQUENCE precedence_constraints_id_seq")
  (exec-raw "DROP TABLE predicate_lookup")
  (exec-raw "DROP TABLE provenance_device")
  (exec-raw "DROP SEQUENCE seq_classificationid_classification")
  (exec-raw "DROP SEQUENCE seq_name_nameid")
  (exec-raw "DROP SEQUENCE seq_namesource_nameid")
  (exec-raw "DROP TABLE statement_type")
  (exec-raw "DROP SEQUENCE step_precedence_id_seq")
  (exec-raw "DROP TABLE thing_type_code"))

(defn convert
  "Performs the conversion for database version 1.4.0:20120525.01."
  []
  (println "Performing conversion for" version)
  (add-edited-date)
  (redefine-analysis-listing)
  (create-analysis-job-types)
  (redefine-version-table)
  (drop-obsolete-tables))
