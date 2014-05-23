SET search_path = public, pg_catalog;

--
-- Updates parameters uuid foreign keys.
-- Adds temporary indexes to help speed up the conversion.
--
CREATE INDEX parameters_validator_idx ON parameters(validator);
CREATE INDEX validator_rule_validator_id_idx ON validator_rule(validator_id);
CREATE INDEX validator_rule_rule_id_idx ON validator_rule(rule_id);

UPDATE validation_rules r SET parameter_id =
    (SELECT p.id FROM parameters p
     LEFT JOIN validator_rule vr ON vr.validator_id = p.validator
     WHERE r.hid = vr.rule_id);

-- Drop temporary indexes.
DROP INDEX parameters_validator_idx;
DROP INDEX validator_rule_validator_id_idx;
DROP INDEX validator_rule_rule_id_idx;

-- Cleanup rows with NULL foreign keys.
DELETE FROM validation_rule_arguments WHERE rule_id IN
  (SELECT id FROM validation_rules WHERE parameter_id IS NULL);
DELETE FROM validation_rules WHERE parameter_id IS NULL;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY validation_rules ALTER COLUMN parameter_id SET NOT NULL;

