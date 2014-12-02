SET search_path = public, pg_catalog;

--
-- Updates columns in the existing suggested_groups table.
--
ALTER TABLE ONLY suggested_groups RENAME COLUMN transformation_activity_id TO transformation_activity_id_v192;
ALTER TABLE ONLY suggested_groups RENAME COLUMN template_group_id TO template_group_id_v192;
ALTER TABLE ONLY suggested_groups ADD COLUMN app_id UUID;
ALTER TABLE ONLY suggested_groups ADD COLUMN app_category_id UUID;

