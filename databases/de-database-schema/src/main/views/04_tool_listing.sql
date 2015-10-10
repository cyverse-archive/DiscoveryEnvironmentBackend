SET search_path = public, pg_catalog;

--
-- A view containing the tool information needed for the app listing service.
--
CREATE VIEW tool_listing AS
    SELECT row_number() OVER (ORDER BY apps.id, steps.step) AS id,
           apps.id AS app_id,
           apps.is_public,
           steps.step AS execution_order,
           tool.id AS tool_id,
           tool."name",
           tool.description,
           tool.location,
           tt."name" AS "type",
           tt.hidden,
           tool.version,
           tool.attribution,
           tool.container_images_id
    FROM app_listing AS apps
         JOIN app_steps steps ON apps.id = steps.app_id
         JOIN tasks t ON steps.task_id = t.id
         JOIN tools tool ON t.tool_id = tool.id
         JOIN tool_types tt ON tool.tool_type_id = tt.id;
