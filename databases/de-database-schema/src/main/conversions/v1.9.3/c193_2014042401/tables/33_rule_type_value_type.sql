SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_type_value_type table.
--
ALTER TABLE ONLY rule_type_value_type RENAME COLUMN rule_type_id TO rule_type_id_v192;
ALTER TABLE ONLY rule_type_value_type RENAME COLUMN value_type_id TO value_type_id_v192;
ALTER TABLE ONLY rule_type_value_type ALTER COLUMN rule_type_id_v192 DROP NOT NULL;
ALTER TABLE ONLY rule_type_value_type ALTER COLUMN value_type_id_v192 DROP NOT NULL;
ALTER TABLE ONLY rule_type_value_type ADD COLUMN rule_type_id UUID;
ALTER TABLE ONLY rule_type_value_type ADD COLUMN value_type_id UUID;

