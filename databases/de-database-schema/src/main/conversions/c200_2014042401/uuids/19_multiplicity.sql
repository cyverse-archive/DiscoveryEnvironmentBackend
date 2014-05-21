SET search_path = public, pg_catalog;

--
-- Updates multiplicity uuid foreign keys.
--
UPDATE file_parameters SET multiplicity =
    (SELECT id FROM multiplicity
     WHERE hid = multiplicity_v187);

