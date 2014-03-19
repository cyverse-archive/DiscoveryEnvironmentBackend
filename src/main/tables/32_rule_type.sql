SET search_path = public, pg_catalog;

--
-- ID sequence for the rule_type table.
--
CREATE SEQUENCE rule_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- rule_type table
--
CREATE TABLE rule_type (
    hid bigint DEFAULT nextval('rule_type_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    deprecated boolean DEFAULT false,
    display_order integer DEFAULT 999,
    rule_description_format character varying(255) DEFAULT '',
    rule_subtype_id bigint
);
