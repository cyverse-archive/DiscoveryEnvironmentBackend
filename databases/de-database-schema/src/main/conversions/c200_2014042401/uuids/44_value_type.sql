SET search_path = public, pg_catalog;

--
-- Updates value_type uuid foreign keys.
--
UPDATE parameter_types SET value_type_id =
    (SELECT v.id FROM value_type v
     WHERE v.hid = value_type_id_v187);

UPDATE rule_type_value_type SET value_type_id =
    (SELECT v.id FROM value_type v
     WHERE v.hid = value_type_id_v187);

