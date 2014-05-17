SET search_path = public, pg_catalog;

--
-- Renames the existing deployed_components table to tools and adds updated columns.
-- cols to drop: hid, id_v187, tool_type_id_v187, integration_data_id_v187
--
ALTER TABLE deployed_components RENAME TO tools;
ALTER TABLE ONLY tools RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY tools ADD COLUMN id UUID DEFAULT (uuid_generate_v4());
UPDATE tools SET id = CAST(id_v187 AS UUID) WHERE CHAR_LENGTH(id_v187) = 36;
ALTER TABLE ONLY tools RENAME COLUMN tool_type_id TO tool_type_id_v187;
ALTER TABLE ONLY tools ADD COLUMN tool_type_id UUID;
ALTER TABLE ONLY tools RENAME COLUMN integration_data_id TO integration_data_id_v187;
ALTER TABLE ONLY tools ADD COLUMN integration_data_id UUID;

