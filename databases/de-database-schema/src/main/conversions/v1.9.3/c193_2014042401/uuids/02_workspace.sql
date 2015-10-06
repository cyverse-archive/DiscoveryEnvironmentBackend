SET search_path = public, pg_catalog;

--
-- Updates workspace uuid foreign keys.
--
UPDATE app_categories SET workspace_id =
    (SELECT w.id FROM workspace w WHERE workspace_id_v192 = w.id_v192);

-- Cleanup rows with NULL foreign keys.
DELETE FROM app_category_group WHERE child_category_id IN
  (SELECT id FROM app_categories WHERE workspace_id IS NULL);
DELETE FROM app_category_group WHERE parent_category_id IN
  (SELECT id FROM app_categories WHERE workspace_id IS NULL);
DELETE FROM suggested_groups WHERE app_category_id IN
  (SELECT id FROM app_categories WHERE workspace_id IS NULL);
DELETE FROM app_category_app WHERE app_category_id IN
  (SELECT id FROM app_categories WHERE workspace_id IS NULL);
DELETE FROM app_categories WHERE workspace_id IS NULL;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY app_categories ALTER COLUMN workspace_id SET NOT NULL;

