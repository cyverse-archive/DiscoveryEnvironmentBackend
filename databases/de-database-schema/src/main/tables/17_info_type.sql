SET search_path = public, pg_catalog;

--
-- ID sequence for the info_type table.
--
CREATE SEQUENCE info_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- A table for information types.
--
CREATE TABLE info_type (
    hid bigint DEFAULT nextval('info_type_id_seq'::regclass) NOT NULL,
    id character varying(40) NOT NULL,
    name character varying(64) NOT NULL,
    label character varying(255),
    description character varying(255),
    deprecated boolean DEFAULT false,
    display_order integer DEFAULT 999
);
