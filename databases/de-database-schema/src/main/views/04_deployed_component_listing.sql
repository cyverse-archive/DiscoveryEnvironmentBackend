SET search_path = public, pg_catalog;

--
-- A view containing the deployed component information needed for the app
-- listing service.
--
CREATE VIEW deployed_component_listing AS
    SELECT row_number() OVER (ORDER BY apps.id, steps.step) AS id,
           apps.id AS app_id,
           steps.step AS execution_order,
           dc.hid AS deployed_component_hid,
           dc.id AS deployed_component_id,
           dc."name",
           dc.description,
           dc.location,
           tt."name" AS "type",
           dc.version,
           dc.attribution
    FROM apps
         JOIN app_steps steps ON apps.id = steps.app_id
         JOIN tasks t ON steps.task_id = t.id
         JOIN deployed_components dc ON t.component_id = dc.id
         JOIN tool_types tt ON dc.tool_type_id = tt.id;
