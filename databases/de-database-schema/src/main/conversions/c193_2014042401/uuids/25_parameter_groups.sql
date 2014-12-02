SET search_path = public, pg_catalog;

--
-- Updates parameter_groups uuid foreign keys.
-- Adds a temporary index to help speed up the conversion.
--
CREATE INDEX property_group_property_property_id_idx ON property_group_property_v192(property_id);
UPDATE parameters p SET parameter_group_id =
    (SELECT g.id FROM parameter_groups g
     LEFT JOIN property_group_property_v192 pgp ON pgp.property_group_id = g.hid_v192
     WHERE p.hid_v192 = pgp.property_id);
DROP INDEX property_group_property_property_id_idx;

-- Cleanup rows with NULL foreign keys.
DELETE FROM validation_rules WHERE parameter_id IN
  (SELECT id FROM parameters WHERE parameter_group_id IS NULL);
DELETE FROM parameters WHERE parameter_group_id IS NULL;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY parameters ALTER COLUMN parameter_group_id SET NOT NULL;

