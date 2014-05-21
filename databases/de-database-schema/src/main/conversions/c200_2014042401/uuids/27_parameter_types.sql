SET search_path = public, pg_catalog;

--
-- Updates parameter_types uuid foreign keys.
--
UPDATE parameters SET parameter_type =
    (SELECT id FROM parameter_types
     WHERE hid = property_type);

UPDATE tool_type_parameter_type SET parameter_type_id =
    (SELECT id FROM parameter_types
     WHERE hid = property_type_id);

