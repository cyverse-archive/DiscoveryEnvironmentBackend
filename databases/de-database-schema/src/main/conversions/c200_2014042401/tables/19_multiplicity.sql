SET search_path = public, pg_catalog;

--
-- Updates columns in the existing multiplicity table.
--
ALTER TABLE ONLY multiplicity ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY multiplicity ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE ONLY multiplicity RENAME COLUMN hid TO hid_v187;

