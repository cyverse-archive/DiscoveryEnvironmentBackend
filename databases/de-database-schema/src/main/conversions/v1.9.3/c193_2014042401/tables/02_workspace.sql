SET search_path = public, pg_catalog;

--
-- Updates columns in the existing workspace table.
--
ALTER TABLE ONLY workspace RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY workspace RENAME COLUMN home_folder TO home_folder_v192;
ALTER TABLE ONLY workspace RENAME COLUMN root_analysis_group_id TO root_analysis_group_id_v192;
ALTER TABLE ONLY workspace RENAME COLUMN user_id TO user_id_v192;
ALTER TABLE ONLY workspace ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY workspace ADD COLUMN root_category_id UUID;
ALTER TABLE ONLY workspace ADD COLUMN user_id UUID;

UPDATE workspace SET id = '00000000-0000-0000-0000-000000000000'
  WHERE id_v192 = 0;
