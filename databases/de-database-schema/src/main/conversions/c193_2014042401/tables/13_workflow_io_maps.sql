SET search_path = public, pg_catalog;

--
-- Renames the existing input_output_mapping table to workflow_io_maps and adds updated columns.
--
ALTER TABLE input_output_mapping RENAME TO workflow_io_maps;

ALTER TABLE ONLY workflow_io_maps RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY workflow_io_maps RENAME COLUMN source TO source_v192;
ALTER TABLE ONLY workflow_io_maps RENAME COLUMN target TO target_v192;
ALTER TABLE ONLY workflow_io_maps ALTER COLUMN source_v192 DROP NOT NULL;
ALTER TABLE ONLY workflow_io_maps ALTER COLUMN target_v192 DROP NOT NULL;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY workflow_io_maps ADD COLUMN app_id UUID;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN target_step UUID;
ALTER TABLE ONLY workflow_io_maps ADD COLUMN source_step UUID;

