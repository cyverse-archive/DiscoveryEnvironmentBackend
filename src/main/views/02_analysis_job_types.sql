SET search_path = public, pg_catalog;

--
-- analysis_job_types view that lists analysis IDs and job types.
--
CREATE VIEW analysis_job_types AS
    SELECT
        a.hid AS analysis_id,
        tt.name AS job_type
    FROM transformation_activity a
        JOIN transformation_task_steps tts ON a.hid = tts.transformation_task_id
        JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
        JOIN transformations tx ON ts.transformation_id = tx.id
        JOIN template t ON tx.template_id::text = t.id::text
        JOIN deployed_components dc ON t.component_id::text = dc.id::text
        JOIN tool_types tt ON dc.tool_type_id = tt.id;

