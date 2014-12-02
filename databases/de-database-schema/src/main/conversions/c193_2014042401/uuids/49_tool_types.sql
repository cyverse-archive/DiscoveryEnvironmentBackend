SET search_path = public, pg_catalog;

--
-- Updates tool_types uuid foreign keys.
--
UPDATE tools SET tool_type_id =
    (SELECT tt.id FROM tool_types tt
     WHERE tt.id_v192 = tool_type_id_v192);

UPDATE tool_type_parameter_type SET tool_type_id =
    (SELECT tt.id FROM tool_types tt
     WHERE tt.id_v192 = tool_type_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY tools ALTER COLUMN tool_type_id SET NOT NULL;
ALTER TABLE ONLY tool_type_parameter_type ALTER COLUMN tool_type_id SET NOT NULL;

