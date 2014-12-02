SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_types table.
--
ALTER TABLE ONLY tool_types RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY tool_types ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY tool_types ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY tool_types ADD COLUMN hidden boolean DEFAULT FALSE;
