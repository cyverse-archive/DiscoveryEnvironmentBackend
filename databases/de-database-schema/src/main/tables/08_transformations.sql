SET search_path = public, pg_catalog;

--
-- id SERIAL type for transformations table
--
CREATE SEQUENCE transformations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- transformations table
--
CREATE TABLE transformations (
    id bigint DEFAULT nextval('transformations_id_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    template_id character varying(255),
    external_app_id character varying(255)
);
