SET search_path = public, pg_catalog;

--
-- Updates app_categories uuid foreign keys.
--
UPDATE workspace SET root_category_id =
    (SELECT ac.id FROM app_categories ac WHERE root_analysis_group_id = ac.hid);
UPDATE app_category_app SET app_category_id =
    (SELECT ac.id FROM app_categories ac WHERE template_group_id = ac.hid);
UPDATE suggested_groups SET app_category_id =
    (SELECT ac.id FROM app_categories ac WHERE template_group_id = ac.hid);
UPDATE app_category_group SET parent_category_id =
    (SELECT ac.id FROM app_categories ac WHERE parent_group_id = ac.hid);
UPDATE app_category_group SET child_category_id =
    (SELECT ac.id FROM app_categories ac WHERE subgroup_id = ac.hid);

