SET search_path = public, pg_catalog;

--
-- The identifier sequence for the tool_requests table.
--
CREATE SEQUENCE tool_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- The tool requests themselves.
--
CREATE TABLE tool_requests (
    id BIGINT DEFAULT nextval('tool_requests_id_seq'::regclass) NOT NULL,
    uuid UUID NOT NULL,
    requestor_id BIGINT NOT NULL,
    phone VARCHAR(30),
    tool_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    source_url TEXT NOT NULL,
    doc_url TEXT NOT NULL,
    version VARCHAR(255) NOT NULL,
    attribution TEXT NOT NULL,
    multithreaded BOOLEAN,
    tool_architecture_id BIGINT REFERENCES tool_architectures(id) NOT NULL,
    test_data_path TEXT NOT NULL,
    instructions TEXT NOT NULL,
    additional_info TEXT,
    additional_data_file TEXT,
    deployed_component_id BIGINT,
    PRIMARY KEY(id)
);
