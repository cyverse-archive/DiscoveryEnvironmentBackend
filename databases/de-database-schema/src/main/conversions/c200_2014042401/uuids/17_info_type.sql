SET search_path = public, pg_catalog;

--
-- Updates info_type uuid foreign keys.
--
UPDATE file_parameters SET info_type =
    (SELECT id FROM info_type
     WHERE hid = info_type_v187);

