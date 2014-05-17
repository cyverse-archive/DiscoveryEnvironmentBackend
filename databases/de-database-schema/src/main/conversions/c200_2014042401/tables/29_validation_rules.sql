SET search_path = public, pg_catalog;

--
-- Renames the existing rule table to validation_rules and adds updated columns.
-- cols to drop: hid, name, description, label, rule_type_v187
--
ALTER TABLE rule RENAME TO validation_rules;
ALTER TABLE ONLY validation_rules
  ALTER COLUMN id TYPE UUID USING
    CASE WHEN CHAR_LENGTH(id) < 36
          THEN (uuid_generate_v4())
          ELSE CAST(id AS UUID)
    END;
ALTER TABLE ONLY validation_rules ADD COLUMN parameter_id UUID;
ALTER TABLE ONLY validation_rules RENAME COLUMN rule_type TO rule_type_v187;
ALTER TABLE ONLY validation_rules ADD COLUMN rule_type UUID;

