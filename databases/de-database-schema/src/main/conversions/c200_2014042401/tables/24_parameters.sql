SET search_path = public, pg_catalog;

--
-- Renames the existing property table to parameters and adds updated columns.
--
ALTER TABLE property RENAME TO parameters;

ALTER TABLE ONLY parameters RENAME COLUMN hid TO hid_v187;
ALTER TABLE ONLY parameters RENAME COLUMN defalut_value TO defalut_value_v187;
ALTER TABLE ONLY parameters RENAME COLUMN property_type TO property_type_v187;
ALTER TABLE ONLY parameters RENAME COLUMN validator TO validator_v187;
ALTER TABLE ONLY parameters RENAME COLUMN dataobject_id TO dataobject_id_v187;
ALTER TABLE ONLY parameters
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v1())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY parameters ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY parameters ALTER COLUMN is_visible SET DEFAULT TRUE;
ALTER TABLE ONLY parameters ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY parameters ADD COLUMN parameter_group_id UUID;
ALTER TABLE ONLY parameters ADD COLUMN parameter_type UUID;
ALTER TABLE ONLY parameters ADD COLUMN required boolean DEFAULT false;
ALTER TABLE ONLY parameters ADD COLUMN file_parameter_id UUID;

WITH dups AS (SELECT id, COUNT(hid_v187) FROM parameters GROUP BY id)
  UPDATE parameters SET id = (uuid_generate_v1())
    WHERE id IN (SELECT id FROM dups WHERE count > 1);

UPDATE parameters SET required = true WHERE id IN
    (SELECT p.id FROM parameters p
     LEFT JOIN validator_v187 v ON v.hid = p.validator_v187
     LEFT JOIN file_parameters f ON f.hid_v187 = p.dataobject_id_v187
     WHERE v.required = true OR f.required_v187 = true);

