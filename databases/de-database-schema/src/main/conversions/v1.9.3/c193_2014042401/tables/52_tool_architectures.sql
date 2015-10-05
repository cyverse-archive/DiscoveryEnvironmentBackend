SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_architectures table.
--
ALTER TABLE ONLY tool_architectures RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY tool_architectures ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY tool_architectures ALTER COLUMN description TYPE TEXT;

