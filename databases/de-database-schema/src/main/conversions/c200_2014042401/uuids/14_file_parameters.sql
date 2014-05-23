SET search_path = public, pg_catalog;

--
-- Updates file_parameters uuid foreign keys.
--
UPDATE parameters SET file_parameter_id =
    (SELECT id FROM file_parameters
     WHERE hid = dataobject_id);
UPDATE input_output_mapping SET input =
    (SELECT id FROM file_parameters
     WHERE id_v187 = input_v187);
UPDATE input_output_mapping SET output =
    (SELECT id FROM file_parameters
     WHERE id_v187 = output_v187);

DELETE FROM input_output_mapping WHERE input IS NULL OR output IS NULL;

-- Add NOT NULL constraints on foreign key columns.
-- parameters.file_parameter_id is NULL for all non-file parameter types.
ALTER TABLE ONLY input_output_mapping ALTER COLUMN input SET NOT NULL;
ALTER TABLE ONLY input_output_mapping ALTER COLUMN output SET NOT NULL;

