SET search_path = public, pg_catalog;

--
-- validation_rules table
--
CREATE TABLE validation_rules (
    id character varying(255) NOT NULL,
    parameter_id character varying(255) NOT NULL,
    rule_type bigint
);
