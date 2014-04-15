SET search_path = public, pg_catalog;

--
-- tool_test_data_files table
--
CREATE TABLE tool_test_data_files (
    id uuid NOT NULL,
    filename character varying(1024) NOT NULL,
    input_file boolean DEFAULT true,
    tool_id uuid NOT NULL
);
