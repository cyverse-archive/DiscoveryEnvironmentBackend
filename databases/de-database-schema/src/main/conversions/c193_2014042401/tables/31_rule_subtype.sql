SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_subtype table.
--
ALTER TABLE ONLY rule_subtype ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY rule_subtype ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY rule_subtype RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY rule_subtype ALTER COLUMN description TYPE TEXT;

