SET search_path = public, pg_catalog;

--
-- Updates tools uuid foreign keys.
--
UPDATE tasks SET tool_id =
    (SELECT t.id FROM tools t WHERE component_id_v192 = t.id_v192);
UPDATE tool_test_data_files SET tool_id =
    (SELECT t.id FROM tools t WHERE deployed_component_id_v192 = t.hid_v192);
UPDATE tool_requests SET tool_id =
    (SELECT t.id FROM tools t WHERE deployed_component_id_v192 = t.hid_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY tool_test_data_files ALTER COLUMN tool_id SET NOT NULL;

