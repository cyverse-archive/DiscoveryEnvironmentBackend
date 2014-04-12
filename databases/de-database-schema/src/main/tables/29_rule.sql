SET search_path = public, pg_catalog;

--
-- ID sequence for the rule table.
--
CREATE SEQUENCE rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- rule table
--
CREATE TABLE rule (
    hid bigint DEFAULT nextval('rule_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    property_id bigint NOT NULL,
    rule_type bigint
);
