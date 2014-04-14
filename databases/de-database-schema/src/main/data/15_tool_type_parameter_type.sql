-- Populates the tool_type_parameter_type table.

INSERT INTO tool_type_parameter_type (tool_type_id, parameter_type_id)
    SELECT tt.id, pt.id
    FROM tool_types tt, parameter_types pt
    WHERE tt."name" = 'executable'
    ORDER BY pt.display_order;

INSERT INTO tool_type_parameter_type (tool_type_id, parameter_type_id)
    SELECT tt.id, pt.id
    FROM tool_types tt, parameter_types pt
    WHERE tt."name" = 'fAPI'
    AND pt."name" != 'EnvironmentVariable'
    ORDER BY pt.display_order;
