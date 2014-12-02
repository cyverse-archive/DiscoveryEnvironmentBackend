SET search_path = public, pg_catalog;

--
-- Updates parameter_types uuid foreign keys.
--
UPDATE parameters SET parameter_type =
    (SELECT id FROM parameter_types pt
     WHERE pt.hid_v192 = property_type_v192);

UPDATE tool_type_parameter_type SET parameter_type_id =
    (SELECT id FROM parameter_types pt
     WHERE pt.hid_v192 = property_type_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY parameters ALTER COLUMN parameter_type SET NOT NULL;
ALTER TABLE ONLY tool_type_parameter_type ALTER COLUMN parameter_type_id SET NOT NULL;

