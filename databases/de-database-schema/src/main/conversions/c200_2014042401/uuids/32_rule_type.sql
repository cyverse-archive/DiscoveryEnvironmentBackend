SET search_path = public, pg_catalog;

--
-- Updates rule_type uuid foreign keys.
--
UPDATE validation_rules SET rule_type =
    (SELECT r.id FROM rule_type r
     WHERE r.hid = rule_type_v187);

UPDATE rule_type_value_type SET rule_type_id =
    (SELECT r.id FROM rule_type r
     WHERE r.hid = rule_type_id_v187);

