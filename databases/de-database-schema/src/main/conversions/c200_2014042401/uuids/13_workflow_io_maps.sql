SET search_path = public, pg_catalog;

--
-- Updates workflow_io_maps uuid foreign keys.
--
UPDATE input_output_mapping SET mapping_id =
    (SELECT id FROM workflow_io_maps
     WHERE hid = mapping_id_v187);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY input_output_mapping ALTER COLUMN mapping_id SET NOT NULL;

