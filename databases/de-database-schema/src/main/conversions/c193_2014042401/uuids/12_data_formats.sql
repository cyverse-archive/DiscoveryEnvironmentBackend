SET search_path = public, pg_catalog;

--
-- Updates data_formats uuid foreign keys.
--
UPDATE file_parameters SET data_format =
    (SELECT d.id FROM data_formats d
     WHERE data_format_v187 = d.id_v187);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY file_parameters ALTER COLUMN data_format SET NOT NULL;

