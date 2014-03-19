SET search_path = public, pg_catalog;

--
-- validator_rule table
--
CREATE TABLE validator_rule (
    validator_id bigint NOT NULL,
    rule_id bigint NOT NULL,
    id integer NOT NULL
);
