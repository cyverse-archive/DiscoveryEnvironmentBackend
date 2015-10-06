SET search_path = public, pg_catalog;

--
-- Updates validation_rules uuid foreign keys.
--
UPDATE validation_rule_arguments SET rule_id =
    (SELECT r.id FROM validation_rules r
     WHERE r.hid_v192 = rule_id_v192);

-- Cleanup rows with NULL foreign keys.
DELETE FROM validation_rule_arguments WHERE rule_id IS NULL;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY validation_rule_arguments ALTER COLUMN rule_id SET NOT NULL;

