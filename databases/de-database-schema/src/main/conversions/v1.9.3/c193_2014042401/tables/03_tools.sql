SET search_path = public, pg_catalog;

--
-- Renames the existing deployed_components table to tools and adds updated columns.
--
ALTER TABLE deployed_components RENAME TO tools;

ALTER TABLE ONLY tools RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY tools RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY tools ALTER COLUMN id_v192 DROP NOT NULL;
ALTER TABLE ONLY tools RENAME COLUMN tool_type_id TO tool_type_id_v192;
ALTER TABLE ONLY tools ALTER COLUMN tool_type_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tools RENAME COLUMN integration_data_id TO integration_data_id_v192;
ALTER TABLE ONLY tools ALTER COLUMN integration_data_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tools ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
UPDATE tools SET id = CAST(id_v192 AS UUID) WHERE CHAR_LENGTH(id_v192) = 36;
ALTER TABLE ONLY tools ADD COLUMN tool_type_id UUID;
ALTER TABLE ONLY tools ADD COLUMN integration_data_id UUID;

