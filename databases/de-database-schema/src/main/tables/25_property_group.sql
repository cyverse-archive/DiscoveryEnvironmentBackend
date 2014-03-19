SET search_path = public, pg_catalog;

--
-- ID sequence for the property_group table.
--
CREATE SEQUENCE property_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- property_group table
--
CREATE TABLE property_group (
    hid bigint DEFAULT nextval('property_group_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    group_type character varying(255),
    is_visible boolean
);
