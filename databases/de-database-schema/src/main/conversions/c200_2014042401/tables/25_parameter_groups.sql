SET search_path = public, pg_catalog;

--
-- Renames the existing property_group table to parameter_groups and adds updated columns.
-- cols to drop: hid, group_type?
--
ALTER TABLE property_group RENAME TO parameter_groups;
ALTER TABLE ONLY parameter_groups
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v4())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY parameter_groups ADD COLUMN task_id UUID;

WITH dups AS (SELECT id, COUNT(hid) FROM parameter_groups GROUP BY id)
  UPDATE parameter_groups SET id = (uuid_generate_v4())
    WHERE id IN (SELECT id FROM dups WHERE count > 1);

