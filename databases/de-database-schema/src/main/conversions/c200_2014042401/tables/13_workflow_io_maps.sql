SET search_path = public, pg_catalog;

--
-- Renames the existing input_output_mapping table to workflow_io_maps and adds updated columns.
-- cols to drop: hid, source, target
--
ALTER TABLE input_output_mapping RENAME TO workflow_io_maps;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN id UUID DEFAULT (uuid_generate_v4());
ALTER TABLE ONLY workflow_io_maps ADD COLUMN app_id UUID;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN target_step UUID;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN source_step UUID;

