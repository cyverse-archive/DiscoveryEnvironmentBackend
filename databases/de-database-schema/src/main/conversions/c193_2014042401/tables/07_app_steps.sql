SET search_path = public, pg_catalog;

--
-- Renames the existing transformation_task_steps table to app_steps and adds updated columns.
--
ALTER TABLE transformation_task_steps RENAME TO app_steps;
ALTER TABLE ONLY app_steps RENAME COLUMN transformation_task_id TO transformation_task_id_v192;
ALTER TABLE ONLY app_steps RENAME COLUMN transformation_step_id TO transformation_step_id_v192;
ALTER TABLE ONLY app_steps ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY app_steps RENAME COLUMN hid TO step;
ALTER TABLE ONLY app_steps ADD COLUMN app_id UUID;
ALTER TABLE ONLY app_steps ADD COLUMN task_id UUID;

