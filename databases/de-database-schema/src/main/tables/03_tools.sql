SET search_path = public, pg_catalog;

--
-- hid SERIAL type for tools table
--
CREATE SEQUENCE tools_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- tools table
--
CREATE TABLE tools (
    hid bigint DEFAULT nextval('tools_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    location character varying(255),
    tool_type_id character varying(255) NOT NULL,
    description text,
    version character varying(255),
    attribution text,
    integration_data_id bigint NOT NULL
);
