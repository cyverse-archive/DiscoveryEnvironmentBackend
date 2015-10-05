SET search_path = public, pg_catalog;

--
-- Updates columns in the existing value_type table.
--
ALTER TABLE ONLY value_type RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY value_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY value_type ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY value_type ALTER COLUMN description TYPE TEXT;

