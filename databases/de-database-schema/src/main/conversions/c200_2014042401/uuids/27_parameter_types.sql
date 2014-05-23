SET search_path = public, pg_catalog;

--
-- Updates parameter_types uuid foreign keys.
--
UPDATE parameters SET parameter_type =
    (SELECT id FROM parameter_types
     WHERE hid = property_type);

UPDATE tool_type_parameter_type SET parameter_type_id =
    (SELECT id FROM parameter_types
     WHERE hid = property_type_id);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY parameters ALTER COLUMN parameter_type SET NOT NULL;
ALTER TABLE ONLY tool_type_parameter_type ALTER COLUMN parameter_type_id SET NOT NULL;

