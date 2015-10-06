SET search_path = public, pg_catalog;

--
-- Updates columns in the existing metadata_value_types table.
--
ALTER TABLE ONLY metadata_value_types ALTER COLUMN id SET DEFAULT uuid_generate_v1();

