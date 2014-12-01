SET search_path = public, pg_catalog;

--
-- Updates integration_data uuid foreign keys.
--
UPDATE tools SET integration_data_id =
    (SELECT i.id FROM integration_data i
     WHERE integration_data_id_v187 = i.id_v187);
UPDATE apps SET integration_data_id =
    (SELECT i.id FROM integration_data i
     WHERE integration_data_id_v187 = i.id_v187);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY tools ALTER COLUMN integration_data_id SET NOT NULL;
ALTER TABLE ONLY apps ALTER COLUMN integration_data_id SET NOT NULL;

