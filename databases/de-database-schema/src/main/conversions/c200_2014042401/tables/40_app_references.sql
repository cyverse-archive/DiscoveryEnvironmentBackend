SET search_path = public, pg_catalog;

--
-- Renames the existing transformation_activity_references table to app_references and adds updated columns.
--
ALTER TABLE transformation_activity_references RENAME TO app_references;
ALTER TABLE ONLY app_references RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY app_references RENAME COLUMN transformation_activity_id TO transformation_activity_id_v187;
ALTER TABLE ONLY app_references ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY app_references ADD COLUMN app_id UUID;

