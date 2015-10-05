SET search_path = public, pg_catalog;

--
-- Renames the existing template_group_group table to app_category_group and adds updated columns.
--
ALTER TABLE template_group_group RENAME TO app_category_group;
ALTER TABLE ONLY app_category_group RENAME COLUMN parent_group_id TO parent_group_id_v192;
ALTER TABLE ONLY app_category_group RENAME COLUMN subgroup_id TO subgroup_id_v192;
ALTER TABLE ONLY app_category_group ALTER COLUMN subgroup_id_v192 DROP NOT NULL;
ALTER TABLE ONLY app_category_group ADD COLUMN parent_category_id UUID;
ALTER TABLE ONLY app_category_group ADD COLUMN child_category_id UUID;
ALTER TABLE ONLY app_category_group RENAME COLUMN hid TO child_index;

