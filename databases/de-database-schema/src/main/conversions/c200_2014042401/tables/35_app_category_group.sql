SET search_path = public, pg_catalog;

--
-- Renames the existing template_group_group table to app_category_group and adds updated columns.
-- cols to drop: hid, parent_group_id, subgroup_id
--
ALTER TABLE template_group_group RENAME TO app_category_group;
ALTER TABLE ONLY app_category_group ADD COLUMN parent_category_id UUID;
ALTER TABLE ONLY app_category_group ADD COLUMN child_category_id UUID;

