SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_types table.
--
ALTER TABLE ONLY tool_types RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY tool_types ADD COLUMN id UUID DEFAULT (uuid_generate_v4());

