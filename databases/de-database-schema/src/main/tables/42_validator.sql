SET search_path = public, pg_catalog;

--
-- ID sequence for the validator table.
--
CREATE SEQUENCE validator_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- validator table
--
CREATE TABLE validator (
    hid bigint DEFAULT nextval('validator_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    required boolean DEFAULT false
);
