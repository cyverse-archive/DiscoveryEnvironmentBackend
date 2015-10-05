SET search_path = public, pg_catalog;

--
-- Renames the existing template_group table to app_categories and adds updated columns.
--
ALTER TABLE template_group RENAME TO app_categories;

UPDATE app_categories SET id = '12c7a585-ec23-3352-e313-02e323112a7c'
  WHERE id = 'g12c7a585ec233352e31302e323112a7ccf18bfd7364';
UPDATE app_categories SET id = '5401bd146c144470aedd57b47ea1b979'
  WHERE id = 'g5401bd146c144470aedd57b47ea1b979';

ALTER TABLE ONLY app_categories RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY app_categories RENAME COLUMN workspace_id TO workspace_id_v192;
ALTER TABLE ONLY app_categories ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY app_categories ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY app_categories ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY app_categories ADD COLUMN workspace_id UUID;

UPDATE app_categories SET id = (uuid_generate_v1())
  WHERE id IS NULL;

