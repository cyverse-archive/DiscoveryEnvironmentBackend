(ns facepalm.c189-2014071401
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20140714.01")

(defn- redefine-analysis-listing-view
  "Redefines the analysis listing view to correct a problem with the incorrect number of steps
   being reported for some apps."
  []
  (println "\t* adding the template_count column to the analysis_listing view")
  (exec-raw "DROP VIEW analysis_listing")
  (exec-raw "CREATE VIEW analysis_listing AS
    SELECT analysis.hid,
           analysis.id,
           analysis.name,
           analysis.description,
           integration.integrator_name,
           integration.integrator_email,
           analysis.integration_date,
           analysis.edited_date,
           analysis.wikiurl,
           (   SELECT CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION)
               FROM ratings
               WHERE transformation_activity_id = analysis.hid
           ) AS average_rating,
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
           COUNT(tx.template_id) AS template_count,
           analysis.deleted,
           analysis.disabled,
           CASE WHEN COUNT(DISTINCT tt.name) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT tt.name) > 1 THEN 'mixed'
                ELSE MAX(tt.name)
           END AS overall_job_type
    FROM transformation_activity analysis
         LEFT JOIN integration_data integration ON analysis.integration_data_id = integration.id
         LEFT JOIN transformation_task_steps tts ON analysis.hid = tts.transformation_task_id
         LEFT JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
         LEFT JOIN transformations tx ON ts.transformation_id = tx.id
         LEFT JOIN template t ON tx.template_id = t.id
         LEFT JOIN deployed_components dc ON t.component_id = dc.id
         LEFT JOIN tool_types tt ON dc.tool_type_id = tt.id
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

(defn convert
  "Performs the database conversion."
  []
  (println "Performing conversion for" version)
  (redefine-analysis-listing-view))
