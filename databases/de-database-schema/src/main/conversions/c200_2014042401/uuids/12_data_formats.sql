SET search_path = public, pg_catalog;

--
-- Updates data_formats uuid foreign keys.
--
UPDATE file_parameters SET data_format =
    (SELECT d.id FROM data_formats d
     WHERE data_format_v187 = d.id_v187);

