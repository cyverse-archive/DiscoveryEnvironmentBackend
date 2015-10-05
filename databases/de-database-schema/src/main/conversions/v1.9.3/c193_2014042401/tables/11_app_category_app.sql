SET search_path = public, pg_catalog;

--
-- Renames the existing template_group_template table to app_category_app and adds updated columns.
--
ALTER TABLE template_group_template RENAME TO app_category_app;
ALTER TABLE ONLY app_category_app RENAME COLUMN template_group_id TO template_group_id_v192;
ALTER TABLE ONLY app_category_app RENAME COLUMN template_id TO template_id_v192;
ALTER TABLE ONLY app_category_app ADD COLUMN app_category_id UUID;
ALTER TABLE ONLY app_category_app ADD COLUMN app_id UUID;

