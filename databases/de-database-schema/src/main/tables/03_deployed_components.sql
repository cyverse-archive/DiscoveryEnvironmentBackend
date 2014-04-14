SET search_path = public, pg_catalog;

--
-- hid SERIAL type for deployed_components table
--
CREATE SEQUENCE deployed_component_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- deployed_components table
--
CREATE TABLE deployed_components (
    hid bigint DEFAULT nextval('deployed_component_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    location character varying(255),
    tool_type_id character varying(255) NOT NULL,
    description text,
    version character varying(255),
    attribution text,
    integration_data_id bigint NOT NULL
);

