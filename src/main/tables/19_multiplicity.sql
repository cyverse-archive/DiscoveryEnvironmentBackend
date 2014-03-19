SET search_path = public, pg_catalog;

--
-- ID sequence for the multiplicity table.
--
CREATE SEQUENCE multiplicity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- A table for our enumeration of multiplicities.
--
CREATE TABLE multiplicity (
    hid bigint DEFAULT nextval('multiplicity_id_seq'::regclass) NOT NULL,
    id character varying(36),
    name character varying(64),
    label character varying(255),
    description character varying(255),
    type_name character varying(64),
    output_type_name character varying(64)
);
