SET search_path = public, pg_catalog;

--
-- Updates validation_rules uuid foreign keys.
--
UPDATE validation_rule_arguments SET rule_id =
    (SELECT r.id FROM validation_rules r
     WHERE r.hid = rule_id_v187);

