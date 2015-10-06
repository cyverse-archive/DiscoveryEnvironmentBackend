SET search_path = public, pg_catalog;

--
-- Updates value_type uuid foreign keys.
--
UPDATE parameter_types SET value_type_id =
    (SELECT v.id FROM value_type v
     WHERE v.hid_v192 = value_type_id_v192);

UPDATE rule_type_value_type SET value_type_id =
    (SELECT v.id FROM value_type v
     WHERE v.hid_v192 = value_type_id_v192);

-- Add NOT NULL constraints on foreign key columns.
-- Some deprecated parameter_types will have a NULL value_type_id.
ALTER TABLE ONLY rule_type_value_type ALTER COLUMN value_type_id SET NOT NULL;

