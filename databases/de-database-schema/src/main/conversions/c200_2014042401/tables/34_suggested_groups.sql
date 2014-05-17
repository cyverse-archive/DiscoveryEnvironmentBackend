SET search_path = public, pg_catalog;

--
-- Updates columns in the existing suggested_groups table.
-- cols to drop: transformation_activity_id, template_group_id
--
ALTER TABLE ONLY suggested_groups ADD COLUMN app_id UUID;
ALTER TABLE ONLY suggested_groups ADD COLUMN app_category_id UUID;

