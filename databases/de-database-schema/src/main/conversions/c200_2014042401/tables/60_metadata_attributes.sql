SET search_path = public, pg_catalog;

--
-- Updates columns in the existing metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY metadata_attributes ALTER COLUMN description TYPE TEXT;

