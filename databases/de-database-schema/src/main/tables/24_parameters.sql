SET search_path = public, pg_catalog;

--
-- ID sequence for the parameters table.
--
CREATE SEQUENCE parameters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- parameters table
--
CREATE TABLE parameters (
    hid bigint DEFAULT nextval('parameters_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    parameter_group_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label text,
    defalut_value character varying(255),
    is_visible boolean,
    ordering integer,
    property_type bigint,
    required boolean DEFAULT false,
    dataobject_id bigint,
    omit_if_blank boolean DEFAULT true
);
