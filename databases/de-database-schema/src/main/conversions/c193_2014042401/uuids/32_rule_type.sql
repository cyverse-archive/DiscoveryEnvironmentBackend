SET search_path = public, pg_catalog;

--
-- Updates rule_type uuid foreign keys.
--
UPDATE validation_rules SET rule_type =
    (SELECT r.id FROM rule_type r
     WHERE r.hid_v192 = rule_type_v192);

UPDATE rule_type_value_type SET rule_type_id =
    (SELECT r.id FROM rule_type r
     WHERE r.hid_v192 = rule_type_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY validation_rules ALTER COLUMN rule_type SET NOT NULL;
ALTER TABLE ONLY rule_type_value_type ALTER COLUMN rule_type_id SET NOT NULL;

