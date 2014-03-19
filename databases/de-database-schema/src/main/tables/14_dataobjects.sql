SET search_path = public, pg_catalog;

--
-- ID sequence for the dataobjects table.
--
CREATE SEQUENCE dataobjects_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- dataobjects table
--
CREATE TABLE dataobjects (
    hid bigint DEFAULT nextval('dataobjects_id_seq'::regclass) NOT NULL,
    id character varying(255),
    name character varying(255),
    label character varying(255),
    orderd integer,
    switch character varying(255),
    info_type bigint,
    data_format bigint,
    description character varying(255),
    required boolean DEFAULT true,
    multiplicity bigint,
    retain boolean DEFAULT false,
    is_implicit boolean DEFAULT false,
    data_source_id bigint
);
