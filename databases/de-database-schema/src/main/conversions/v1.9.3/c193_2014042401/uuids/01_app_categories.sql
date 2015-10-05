SET search_path = public, pg_catalog;

--
-- Updates app_categories uuid foreign keys.
--
UPDATE workspace SET root_category_id =
    (SELECT ac.id FROM app_categories ac WHERE root_analysis_group_id_v192 = ac.hid_v192);
UPDATE app_category_app SET app_category_id =
    (SELECT ac.id FROM app_categories ac WHERE template_group_id_v192 = ac.hid_v192);
UPDATE suggested_groups SET app_category_id =
    (SELECT ac.id FROM app_categories ac WHERE template_group_id_v192 = ac.hid_v192);
UPDATE app_category_group SET parent_category_id =
    (SELECT ac.id FROM app_categories ac WHERE parent_group_id_v192 = ac.hid_v192);
UPDATE app_category_group SET child_category_id =
    (SELECT ac.id FROM app_categories ac WHERE subgroup_id_v192 = ac.hid_v192);

-- Cleanup rows with NULL foreign keys.
DELETE FROM workspace WHERE root_category_id IS NULL;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY app_category_app ALTER COLUMN app_category_id SET NOT NULL;
ALTER TABLE ONLY suggested_groups ALTER COLUMN app_category_id SET NOT NULL;
ALTER TABLE ONLY app_category_group ALTER COLUMN parent_category_id SET NOT NULL;
ALTER TABLE ONLY app_category_group ALTER COLUMN child_category_id SET NOT NULL;

