SET search_path = public, pg_catalog;

--
-- ID sequence for the validation_rules table.
--
CREATE SEQUENCE validation_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- validation_rules table
--
CREATE TABLE validation_rules (
    hid bigint DEFAULT nextval('validation_rules_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    parameter_id character varying(255) NOT NULL,
    rule_type bigint
);
