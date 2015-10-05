SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_type table.
--
ALTER TABLE ONLY rule_type RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY rule_type RENAME COLUMN rule_subtype_id TO rule_subtype_id_v192;
ALTER TABLE ONLY rule_type ALTER COLUMN id TYPE UUID USING
 CAST(regexp_replace(id, '^rt', '') AS UUID);
ALTER TABLE ONLY rule_type ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY rule_type ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY rule_type ADD COLUMN rule_subtype_id UUID;

