SET search_path = public, pg_catalog;

--
-- A view containing the top-level information needed for the app listing
-- service.
--
CREATE VIEW app_listing AS
    SELECT apps.id,
           apps."name",
           apps.description,
           integration.integrator_name,
           integration.integrator_email,
           apps.integration_date,
           apps.edited_date,
           apps.wikiurl,
           (   SELECT CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION)
               FROM ratings
               WHERE app_id = apps.id
           ) AS average_rating,
           EXISTS (
               SELECT *
               FROM template_group_template tgt
               JOIN app_categories ac ON tgt.app_category_id = ac.id
               JOIN workspace w ON ac.workspace_id = w.id
               WHERE apps.id = tgt.app_id
               AND w.is_public IS TRUE
           ) AS is_public,
           COUNT(tts.*) AS step_count,
           COUNT(t.component_id) AS component_count,
           apps.deleted,
           apps.disabled,
           CASE WHEN COUNT(DISTINCT tt.name) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT tt.name) > 1 THEN 'mixed'
                ELSE MAX(tt.name)
           END AS overall_job_type
    FROM apps
         LEFT JOIN integration_data integration ON apps.integration_data_id = integration.id
         LEFT JOIN transformation_task_steps tts ON apps.id = tts.app_id
         LEFT JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
         LEFT JOIN transformations tx ON ts.transformation_id = tx.id
         LEFT JOIN template t ON tx.template_id = t.id
         LEFT JOIN deployed_components dc ON t.component_id = dc.id
         LEFT JOIN tool_types tt ON dc.tool_type_id = tt.id
    GROUP BY apps.id,
             apps."name",
             apps.description,
             integration.integrator_name,
             integration.integrator_email,
             apps.integration_date,
             apps.edited_date,
             apps.wikiurl,
             apps.deleted,
             apps.disabled;
