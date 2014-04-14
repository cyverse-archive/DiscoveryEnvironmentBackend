-- Populates the tool_type_parameter_type table.

INSERT INTO tool_type_parameter_type (tool_type_id, property_type_id)
    SELECT tt.id, pt.hid
    FROM tool_types tt, parameter_types pt
    WHERE tt."name" = 'executable'
    ORDER BY pt.hid;

INSERT INTO tool_type_parameter_type (tool_type_id, property_type_id)
    SELECT tt.id, pt.hid
    FROM tool_types tt, parameter_types pt
    WHERE tt."name" = 'fAPI'
    AND pt."name" != 'EnvironmentVariable'
    ORDER BY pt.hid;
