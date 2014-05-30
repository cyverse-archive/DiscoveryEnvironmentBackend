SET search_path = public, pg_catalog;

--
-- Updates columns in the existing integration_data table.
--
ALTER TABLE ONLY integration_data RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY integration_data ADD COLUMN id UUID DEFAULT (uuid_generate_v4());

