SET search_path = public, pg_catalog;

--
-- Add new tool types to the database.
--
INSERT INTO tool_types (id, name, label, description, hidden)
    VALUES ( '01E14110-1420-4DE0-8A70-B0DD420F6A84', 'internal', 'Internal DE tools.',
             'Tools used internally by the Discovery Environment.', true );

--
-- Add associations between tool types and compatible parameter types.
--
INSERT INTO tool_type_parameter_type (tool_type_id, parameter_type_id)
    SELECT tt.id, pt.id
    FROM tool_types tt, parameter_types pt
    WHERE tt."name" = 'internal'
    ORDER BY pt.display_order;
