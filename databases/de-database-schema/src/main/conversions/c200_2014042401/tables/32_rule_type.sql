SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_type table.
-- cols to drop: hid
--
ALTER TABLE ONLY rule_type ALTER COLUMN id TYPE UUID USING
 CAST(regexp_replace(id, 'rt(.*)', '\1') AS UUID);
ALTER TABLE ONLY rule_type RENAME COLUMN rule_subtype_id TO rule_subtype_id_v187;
ALTER TABLE ONLY rule_type ADD COLUMN rule_subtype_id UUID;

