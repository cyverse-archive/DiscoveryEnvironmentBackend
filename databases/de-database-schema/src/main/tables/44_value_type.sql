SET search_path = public, pg_catalog;

--
-- ID sequence for the value_type table.
--
CREATE SEQUENCE value_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- A table to store value types associated with various parameter types.
--
CREATE TABLE value_type (
    hid bigint DEFAULT nextval('value_type_id_seq'::regclass) NOT NULL,
    id character varying(40) NOT NULL,
    name character varying(40) NOT NULL,
    description character varying(255) NOT NULL
);
