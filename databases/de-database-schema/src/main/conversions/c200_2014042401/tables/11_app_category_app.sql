SET search_path = public, pg_catalog;

--
-- Renames the existing template_group_template table to app_category_app and adds updated columns.
-- cols to drop: template_group_id, template_id
--
ALTER TABLE template_group_template RENAME TO app_category_app;
ALTER TABLE ONLY app_category_app ADD COLUMN app_category_id UUID;
ALTER TABLE ONLY app_category_app ADD COLUMN app_id UUID;

