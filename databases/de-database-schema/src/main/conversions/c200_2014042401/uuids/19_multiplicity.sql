SET search_path = public, pg_catalog;

--
-- Updates multiplicity uuid foreign keys.
--
UPDATE file_parameters SET multiplicity =
    (SELECT id FROM multiplicity
     WHERE hid = multiplicity_v187);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY file_parameters ALTER COLUMN multiplicity SET NOT NULL;

