SET search_path = public, pg_catalog;

--
-- Renames the existing property table to parameters and adds updated columns.
-- cols to drop: hid, defalut_value, property_type, validator, dataobject_id
--
ALTER TABLE property RENAME TO parameters;
ALTER TABLE ONLY parameters
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v4())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY parameters ADD COLUMN parameter_group_id UUID;
ALTER TABLE ONLY parameters ADD COLUMN parameter_type UUID;
ALTER TABLE ONLY parameters ADD COLUMN required boolean DEFAULT false;
ALTER TABLE ONLY parameters ADD COLUMN file_parameter_id UUID;

WITH dups AS (SELECT id, COUNT(hid) FROM parameters GROUP BY id)
  UPDATE parameters SET id = (uuid_generate_v4())
    WHERE id IN (SELECT id FROM dups WHERE count > 1);

