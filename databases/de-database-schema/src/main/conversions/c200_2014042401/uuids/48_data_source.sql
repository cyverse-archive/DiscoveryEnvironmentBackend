SET search_path = public, pg_catalog;

--
-- Updates data_source uuid foreign keys.
--
UPDATE file_parameters SET data_source_id =
    (SELECT d.id FROM data_source d
     WHERE d.id_v187 = data_source_id_v187);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY file_parameters ALTER COLUMN data_source_id SET NOT NULL;

