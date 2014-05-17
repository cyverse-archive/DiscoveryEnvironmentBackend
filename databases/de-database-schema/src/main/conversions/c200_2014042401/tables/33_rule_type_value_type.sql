SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_type_value_type table.
-- cols to drop: rule_type_id_v187, value_type_id_v187
--
ALTER TABLE ONLY rule_type_value_type RENAME COLUMN rule_type_id TO rule_type_id_v187;
ALTER TABLE ONLY rule_type_value_type ADD COLUMN rule_type_id UUID;
ALTER TABLE ONLY rule_type_value_type RENAME COLUMN value_type_id TO value_type_id_v187;
ALTER TABLE ONLY rule_type_value_type ADD COLUMN value_type_id UUID;

