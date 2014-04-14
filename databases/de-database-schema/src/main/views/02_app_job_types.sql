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
        JOIN tools tool ON t.component_id = tool.id
        JOIN tool_types tt ON tool.tool_type_id = tt.id;
