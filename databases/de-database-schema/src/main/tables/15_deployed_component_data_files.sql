SET search_path = public, pg_catalog;

--
-- id SERIAL type for deployed_component_data_files table
--
CREATE SEQUENCE deployed_component_data_files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- deployed_component_data_files table
--
CREATE TABLE deployed_component_data_files (
    id bigint DEFAULT nextval('deployed_component_data_files_id_seq'::regclass) NOT NULL,
    filename character varying(1024) NOT NULL,
    input_file boolean DEFAULT true,
    deployed_component_id bigint
);
