SET search_path = public, pg_catalog;

--
-- Renames the existing deployed_component_data_files table to tool_test_data_files and adds updated columns.
-- cols to drop: id_v187, deployed_component_id
--
ALTER TABLE deployed_component_data_files RENAME TO tool_test_data_files;
ALTER TABLE ONLY tool_test_data_files RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY tool_test_data_files ADD COLUMN id UUID DEFAULT (uuid_generate_v4());
ALTER TABLE ONLY tool_test_data_files ADD COLUMN tool_id UUID;

