SET search_path = public, pg_catalog;

--
-- Updates columns in the existing info_type table.
--
ALTER TABLE ONLY info_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY info_type ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY info_type RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY info_type ALTER COLUMN description TYPE TEXT;

