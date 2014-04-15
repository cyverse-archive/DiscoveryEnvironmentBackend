SET search_path = public, pg_catalog;

--
-- The tool requests themselves.
--
CREATE TABLE tool_requests (
    id UUID NOT NULL,
    requestor_id UUID NOT NULL,
    phone VARCHAR(30),
    tool_name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    source_url TEXT NOT NULL,
    doc_url TEXT NOT NULL,
    version VARCHAR(255) NOT NULL,
    attribution TEXT NOT NULL,
    multithreaded BOOLEAN,
    tool_architecture_id UUID REFERENCES tool_architectures(id) NOT NULL,
    test_data_path TEXT NOT NULL,
    instructions TEXT NOT NULL,
    additional_info TEXT,
    additional_data_file TEXT,
    tool_id UUID,
    PRIMARY KEY(id)
);
