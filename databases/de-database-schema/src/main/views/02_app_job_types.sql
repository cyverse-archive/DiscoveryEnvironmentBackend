SET search_path = public, pg_catalog;

--
-- app_job_types view that lists app IDs and job types.
--
CREATE VIEW app_job_types AS
    SELECT
        apps.id AS app_id,
        tt.name AS job_type
    FROM apps
        JOIN app_steps steps ON apps.id = steps.app_id
        JOIN tasks t ON steps.task_id = t.id
        JOIN deployed_components dc ON t.component_id::text = dc.id::text
        JOIN tool_types tt ON dc.tool_type_id = tt.id;
