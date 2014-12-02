SET search_path = public, pg_catalog;

--
-- Updates parameters uuid foreign keys.
-- Adds temporary indexes to help speed up the conversion.
--
CREATE INDEX parameters_dataobject_id_idx ON parameters(validator_v192);
CREATE INDEX parameters_validator_idx ON parameters(validator_v192);
CREATE INDEX validator_rule_validator_id_idx ON validator_rule_v192(validator_id);
CREATE INDEX validator_rule_rule_id_idx ON validator_rule_v192(rule_id);

UPDATE file_parameters f SET parameter_id =
    (SELECT id FROM parameters p
     WHERE p.dataobject_id_v192 = f.hid_v192);

UPDATE validation_rules r SET parameter_id =
    (SELECT p.id FROM parameters p
     LEFT JOIN validator_rule_v192 vr ON vr.validator_id = p.validator_v192
     WHERE r.hid_v192 = vr.rule_id);

-- Copy input/output mappings for DE parameters to the new fields.
UPDATE input_output_mapping iom SET input = p.id
    FROM parameters p JOIN file_parameters f ON f.parameter_id = p.id
    WHERE f.id_v192 = input_v192;
UPDATE input_output_mapping iom SET output = p.id
    FROM parameters P JOIN file_parameters f ON f.parameter_id = p.id
    WHERE f.id_v192 = output_v192;

-- Copy input/output mappings for external parameters to the new fields.
UPDATE input_output_mapping SET external_input = input_v192
    WHERE input IS NULL;
UPDATE input_output_mapping SET external_output = output_v192
    WHERE output IS NULL;

-- Drop temporary indexes.
DROP INDEX parameters_validator_idx;
DROP INDEX validator_rule_validator_id_idx;
DROP INDEX validator_rule_rule_id_idx;

-- Cleanup rows with NULL foreign keys.
ALTER TABLE validation_rule_arguments DROP CONSTRAINT rule_argument_rule_id_fkey;
ALTER TABLE validator_rule_v192 DROP CONSTRAINT validator_rule_rule_id_fkey;
DELETE FROM validation_rules WHERE parameter_id IS NULL;

-- Add NOT NULL constraints on required foreign key columns.
ALTER TABLE ONLY file_parameters ALTER COLUMN parameter_id SET NOT NULL;
ALTER TABLE ONLY validation_rules ALTER COLUMN parameter_id SET NOT NULL;
