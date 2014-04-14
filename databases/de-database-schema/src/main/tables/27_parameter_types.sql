SET search_path = public, pg_catalog;

--
-- ID sequence for the parameter_types table.
--
CREATE SEQUENCE parameter_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- parameter_types table
--
CREATE TABLE parameter_types (
    hid bigint DEFAULT nextval('parameter_types_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    deprecated boolean DEFAULT false,
    hidable boolean DEFAULT false,
    display_order integer DEFAULT 999,
    value_type_id bigint
);
