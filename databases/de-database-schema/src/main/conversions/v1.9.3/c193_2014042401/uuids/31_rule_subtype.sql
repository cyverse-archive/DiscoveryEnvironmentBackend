SET search_path = public, pg_catalog;

--
-- Updates rule_subtype uuid foreign keys.
--
UPDATE rule_type SET rule_subtype_id =
    (SELECT s.id FROM rule_subtype s
     WHERE s.hid_v192 = rule_subtype_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY rule_type ALTER COLUMN rule_subtype_id SET NOT NULL;

