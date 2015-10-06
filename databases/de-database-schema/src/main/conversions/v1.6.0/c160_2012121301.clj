(ns facepalm.c160-2012121301
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.6.0:20121213.01")

(defn- redefine-analysis-listing-view
  "Redefines the analysis listing view so that it includes a component_count column."
  []
  (println "\t* adding the component_count column to the analysis_listing view")
  (exec-raw "DROP VIEW analysis_listing")
  (exec-raw
   "CREATE VIEW analysis_listing AS
    SELECT analysis.hid,
           analysis.id,
           analysis.\"name\",
           analysis.description,
           integration.integrator_name,
           integration.integrator_email,
           analysis.integration_date,
           analysis.edited_date,
           analysis.wikiurl,
           CAST(COALESCE(AVG(ratings.rating), 0.0) AS DOUBLE PRECISION) AS average_rating,
           EXISTS (
               SELECT *
               FROM template_group_template tgt
               JOIN template_group tg ON tgt.template_group_id = tg.hid
               JOIN workspace w ON tg.workspace_id = w.id
               WHERE analysis.hid = tgt.template_id
               AND w.is_public IS TRUE
           ) AS is_public,
           COUNT(tts.*) AS step_count,
           COUNT(t.component_id) AS component_count,
           analysis.deleted,
           analysis.disabled,
           CASE WHEN COUNT(DISTINCT tt.name) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT tt.name) > 1 THEN 'mixed'
                ELSE MAX(tt.name)
           END AS overall_job_type
    FROM transformation_activity analysis
         LEFT JOIN integration_data integration ON analysis.integration_data_id = integration.id
         LEFT JOIN ratings ON analysis.hid = ratings.transformation_activity_id
         LEFT JOIN transformation_task_steps tts ON analysis.hid = tts.transformation_task_id
         LEFT JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
         LEFT JOIN transformations tx ON ts.transformation_id = tx.id
         LEFT JOIN template t ON tx.template_id = t.id
         LEFT JOIN deployed_components dc ON t.component_id = dc.id
         LEFT JOIN tool_types tt ON dc.tool_type_id = tt.id
    GROUP BY analysis.hid,
             analysis.id,
             analysis.\"name\",
             analysis.description,
             integration.integrator_name,
             integration.integrator_email,
             analysis.integration_date,
             analysis.edited_date,
             analysis.wikiurl,
             analysis.deleted,
             analysis.disabled"))

(defn convert
  "Performs the conversion for database version 1.6.0:20121213.01."
  []
  (println "Performing conversion for" version)
  (redefine-analysis-listing-view))
