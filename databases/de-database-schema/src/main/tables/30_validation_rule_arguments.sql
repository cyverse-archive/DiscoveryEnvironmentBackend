SET search_path = public, pg_catalog;

--
-- validation_rule_arguments table
--
CREATE TABLE validation_rule_arguments (
    id character varying(255) NOT NULL,
    rule_id character varying(255) NOT NULL,
    argument_value text
);
