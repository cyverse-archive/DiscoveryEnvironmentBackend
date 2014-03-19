SET search_path = public, pg_catalog;

--
-- hid SERIAL type for template_group table
--
CREATE SEQUENCE template_group_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- template_group table
--
CREATE TABLE template_group (
    hid bigint DEFAULT nextval('template_group_id_seq'::regclass) NOT NULL,
    id character varying(255),
    name character varying(255),
    description character varying(255),
    workspace_id bigint
);

