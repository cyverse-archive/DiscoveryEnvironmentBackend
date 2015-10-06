SET search_path = public, pg_catalog;

--
-- A view containing the top-level information needed for the app listing
-- service.
--
CREATE VIEW app_listing AS
    SELECT apps.id,
           apps."name",
           lower(apps."name") AS lower_case_name,
           apps.description,
           integration.integrator_name,
           integration.integrator_email,
           apps.integration_date,
           apps.edited_date,
           apps.wiki_url,
           (   SELECT CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION)
               FROM ratings
               WHERE app_id = apps.id
           ) AS average_rating,
           (   SELECT COUNT(rating)
               FROM ratings
               WHERE app_id = apps.id
           ) AS total_ratings,
           EXISTS (
               SELECT *
               FROM app_category_app aca
               JOIN app_categories ac ON aca.app_category_id = ac.id
               JOIN workspace w ON ac.workspace_id = w.id
               WHERE apps.id = aca.app_id
               AND w.is_public IS TRUE
           ) AS is_public,
           COUNT(steps.*) AS step_count,
           COUNT(t.tool_id) AS tool_count,
           COUNT(t.external_app_id) AS external_app_count,
           COUNT(t.id) AS task_count,
           apps.deleted,
           apps.disabled,
           CASE WHEN COUNT(DISTINCT tt.name) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT tt.name) > 1 THEN 'mixed'
                ELSE MAX(tt.name)
           END AS overall_job_type
    FROM apps
         LEFT JOIN integration_data integration ON apps.integration_data_id = integration.id
         LEFT JOIN app_steps steps ON apps.id = steps.app_id
         LEFT JOIN tasks t ON steps.task_id = t.id
         LEFT JOIN tools tool ON t.tool_id = tool.id
         LEFT JOIN tool_types tt ON tool.tool_type_id = tt.id
    GROUP BY apps.id,
             apps."name",
             apps.description,
             integration.integrator_name,
             integration.integrator_email,
             apps.integration_date,
             apps.edited_date,
             apps.wiki_url,
             apps.deleted,
             apps.disabled;
