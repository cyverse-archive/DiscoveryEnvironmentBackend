SET search_path = public, pg_catalog;

--
-- Updates parameters uuid foreign keys.
--
UPDATE validation_rules SET parameter_id =
    (SELECT p.id FROM parameters p
     LEFT JOIN validator_rule vr ON vr.validator_id = p.validator
     WHERE hid = rule_id);

