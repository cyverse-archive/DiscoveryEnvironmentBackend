SET search_path = public, pg_catalog;

--
-- Updates file_parameters uuid foreign keys.
--
UPDATE parameters SET file_parameter_id =
    (SELECT id FROM file_parameters f
     WHERE f.hid_v187 = dataobject_id_v187);
