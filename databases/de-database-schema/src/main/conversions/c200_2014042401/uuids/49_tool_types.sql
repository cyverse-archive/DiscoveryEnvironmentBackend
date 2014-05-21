SET search_path = public, pg_catalog;

--
-- Updates tool_types uuid foreign keys.
--
UPDATE tools SET tool_type_id =
    (SELECT tt.id FROM tool_types tt
     WHERE tt.id_v187 = tool_type_id_v187);

UPDATE tool_type_parameter_type SET tool_type_id =
    (SELECT tt.id FROM tool_types tt
     WHERE tt.id_v187 = tool_type_id_v187);

