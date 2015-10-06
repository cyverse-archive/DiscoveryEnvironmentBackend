SET search_path = public, pg_catalog;

--
-- Renames the existing transformation_activity table to apps and adds updated columns."
--
ALTER TABLE transformation_activity RENAME TO apps;

ALTER TABLE ONLY apps RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY apps RENAME COLUMN workspace_id TO workspace_id_v192;
ALTER TABLE ONLY apps ALTER COLUMN workspace_id_v192 DROP NOT NULL;
ALTER TABLE ONLY apps RENAME COLUMN type TO type_v192;
ALTER TABLE ONLY apps RENAME COLUMN integration_data_id TO integration_data_id_v192;
ALTER TABLE ONLY apps ALTER COLUMN integration_data_id_v192 DROP NOT NULL;
ALTER TABLE ONLY apps RENAME COLUMN wikiurl TO wiki_url;
ALTER TABLE ONLY apps ADD COLUMN id_v192 CHARACTER VARYING(255);
UPDATE apps SET id_v192 = id;

ALTER TABLE ONLY apps
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v1())
          ELSE CAST(id AS UUID)
    END;

ALTER TABLE ONLY apps ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY apps ADD COLUMN integration_data_id UUID;
ALTER TABLE ONLY apps ALTER COLUMN deleted SET DEFAULT false;
ALTER TABLE ONLY apps ALTER COLUMN deleted SET NOT NULL;
ALTER TABLE ONLY apps ALTER COLUMN description TYPE TEXT;
