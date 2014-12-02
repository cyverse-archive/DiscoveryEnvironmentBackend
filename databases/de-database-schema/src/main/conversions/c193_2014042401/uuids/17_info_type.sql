SET search_path = public, pg_catalog;

--
-- Updates info_type uuid foreign keys.
--
UPDATE file_parameters SET info_type =
    (SELECT id FROM info_type i
     WHERE i.hid_v192 = info_type_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY file_parameters ALTER COLUMN info_type SET NOT NULL;

