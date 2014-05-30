SET search_path = public, pg_catalog;

--
-- Renames the existing transformation_activity table to apps and adds updated columns."
--
ALTER TABLE transformation_activity RENAME TO apps;
ALTER TABLE ONLY apps RENAME COLUMN hid TO hid_v187;
ALTER TABLE ONLY apps RENAME COLUMN workspace_id TO workspace_id_v187;
ALTER TABLE ONLY apps RENAME COLUMN type TO type_v187;
ALTER TABLE ONLY apps RENAME COLUMN integration_data_id TO integration_data_id_v187;
ALTER TABLE ONLY apps
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v4())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY apps ADD COLUMN integration_data_id UUID;
ALTER TABLE ONLY apps ALTER COLUMN deleted SET DEFAULT false;
ALTER TABLE ONLY apps ALTER COLUMN deleted SET NOT NULL;

