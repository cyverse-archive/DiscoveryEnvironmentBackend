SET search_path = public, pg_catalog;

--
-- Renames the existing property_group table to parameter_groups and adds updated columns.
--
ALTER TABLE property_group RENAME TO parameter_groups;

ALTER TABLE ONLY parameter_groups RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY parameter_groups RENAME COLUMN group_type TO group_type_v192;
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
ALTER TABLE ONLY parameter_groups ADD COLUMN display_order int NOT NULL DEFAULT 0;

-- Adds temporary indexes to help speed up the conversion.
CREATE INDEX template_property_group_property_group_id_idx ON template_property_group_v192(property_group_id);
UPDATE parameter_groups SET display_order =
  (SELECT hid FROM template_property_group_v192 WHERE property_group_id = hid_v192);
-- Drop temporary indexes.
DROP INDEX template_property_group_property_group_id_idx;

WITH dups AS (SELECT id, COUNT(hid_v192) FROM parameter_groups GROUP BY id)
  UPDATE parameter_groups SET id = (uuid_generate_v1())
    WHERE id IN (SELECT id FROM dups WHERE count > 1);

-- CORE-6155 The group "label" has been used instead of its "name" by the client for a long time.
UPDATE parameter_groups SET label = name WHERE label IS NULL;
ALTER TABLE ONLY parameter_groups ALTER COLUMN label SET NOT NULL;
ALTER TABLE ONLY parameter_groups ALTER COLUMN name DROP NOT NULL;
