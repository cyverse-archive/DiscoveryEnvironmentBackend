SET search_path = public, pg_catalog;

--
-- Updates columns in the existing metadata_templates table.
--
ALTER TABLE ONLY metadata_templates ALTER COLUMN id SET DEFAULT uuid_generate_v1();

