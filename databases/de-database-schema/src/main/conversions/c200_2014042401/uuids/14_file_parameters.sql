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

