SET search_path = public, pg_catalog;

--
-- Renames the existing rule_argument table to validation_rule_arguments and adds updated columns.
-- cols to drop: hid, rule_id_v187
--
ALTER TABLE rule_argument RENAME TO validation_rule_arguments;
ALTER TABLE ONLY validation_rule_arguments ADD COLUMN id UUID DEFAULT (uuid_generate_v4());
ALTER TABLE ONLY validation_rule_arguments RENAME COLUMN rule_id TO rule_id_v187;
ALTER TABLE ONLY validation_rule_arguments ADD COLUMN rule_id UUID;

