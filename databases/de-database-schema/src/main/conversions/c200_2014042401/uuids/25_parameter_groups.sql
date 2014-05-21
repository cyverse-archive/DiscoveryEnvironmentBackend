SET search_path = public, pg_catalog;

--
-- Updates parameter_groups uuid foreign keys.
-- Adds a temporary index to help speed up the conversion.
--
CREATE INDEX property_group_property_property_id_idx ON property_group_property(property_id);
UPDATE parameters p SET parameter_group_id =
    (SELECT g.id FROM parameter_groups g
     LEFT JOIN property_group_property pgp ON pgp.property_group_id = g.hid
     WHERE p.hid = pgp.property_id);
DROP INDEX property_group_property_property_id_idx;

