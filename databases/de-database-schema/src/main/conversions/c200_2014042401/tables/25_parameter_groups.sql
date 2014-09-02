SET search_path = public, pg_catalog;

--
-- Renames the existing property_group table to parameter_groups and adds updated columns.
--
ALTER TABLE property_group RENAME TO parameter_groups;

ALTER TABLE ONLY parameter_groups RENAME COLUMN hid TO hid_v187;
ALTER TABLE ONLY parameter_groups RENAME COLUMN group_type TO group_type_v187;
ALTER TABLE ONLY parameter_groups
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v1())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY parameter_groups ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY parameter_groups ALTER COLUMN is_visible SET DEFAULT TRUE;
ALTER TABLE ONLY parameter_groups ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY parameter_groups ADD COLUMN task_id UUID;

WITH dups AS (SELECT id, COUNT(hid_v187) FROM parameter_groups GROUP BY id)
  UPDATE parameter_groups SET id = (uuid_generate_v1())
    WHERE id IN (SELECT id FROM dups WHERE count > 1);

