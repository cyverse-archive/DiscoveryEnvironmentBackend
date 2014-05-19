SET search_path = public, pg_catalog;

--
-- Updates tools uuid foreign keys.
--
UPDATE tasks SET tool_id =
    (SELECT t.id FROM tools t WHERE component_id = t.id_v187);
UPDATE tool_test_data_files SET tool_id =
    (SELECT t.id FROM tools t WHERE deployed_component_id = t.hid);
UPDATE tool_requests SET tool_id =
    (SELECT t.id FROM tools t WHERE deployed_component_id = t.hid);

