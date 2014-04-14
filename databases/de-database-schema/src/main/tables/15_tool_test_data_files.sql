SET search_path = public, pg_catalog;

--
-- id SERIAL type for tool_test_data_files table
--
CREATE SEQUENCE tool_test_data_files_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- tool_test_data_files table
--
CREATE TABLE tool_test_data_files (
    id bigint DEFAULT nextval('tool_test_data_files_id_seq'::regclass) NOT NULL,
    filename character varying(1024) NOT NULL,
    input_file boolean DEFAULT true,
    tool_id character varying(255)
);
