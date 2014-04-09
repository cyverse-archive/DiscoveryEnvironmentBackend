SET search_path = public, pg_catalog;

--
-- hid SERIAL type for app_categories table
--
CREATE SEQUENCE app_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- app_categories table
--
CREATE TABLE app_categories (
    hid bigint DEFAULT nextval('app_categories_id_seq'::regclass) NOT NULL,
    id character varying(255),
    name character varying(255),
    description character varying(255),
    workspace_id bigint
);
