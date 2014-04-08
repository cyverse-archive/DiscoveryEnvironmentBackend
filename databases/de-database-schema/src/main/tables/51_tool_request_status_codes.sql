SET search_path = public, pg_catalog;

--
-- A table defining the set of available tool request status codes.
--
CREATE TABLE tool_request_status_codes (
    id UUID NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(128) NOT NULL,
    email_template VARCHAR(64),
    PRIMARY KEY(id)
);

--
-- All tool request status code names should be unique.
--
CREATE UNIQUE INDEX tool_request_status_codes_name_index
    on tool_request_status_codes (name);
