SET search_path = public, pg_catalog;

--
-- Renames the existing template table to tasks and adds updated columns.
-- cols to drop: hid, component_id
--
ALTER TABLE template RENAME TO tasks;
ALTER TABLE ONLY tasks RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY tasks ADD COLUMN id UUID DEFAULT (uuid_generate_v4());
UPDATE tasks SET id = CAST(id_v187 AS UUID) WHERE CHAR_LENGTH(id_v187) = 36;
ALTER TABLE ONLY tasks ADD COLUMN tool_id UUID;

