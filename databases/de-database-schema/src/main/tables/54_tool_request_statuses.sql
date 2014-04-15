SET search_path = public, pg_catalog;

--
-- The statuses that have been applied to each tool request.
--
CREATE TABLE tool_request_statuses (
    id UUID NOT NULL,
    tool_request_id UUID REFERENCES tool_requests(id) NOT NULL,
    tool_request_status_code_id UUID REFERENCES tool_request_status_codes(id) NOT NULL,
    date_assigned TIMESTAMP DEFAULT now() NOT NULL,
    updater_id UUID NOT NULL,
    comments TEXT,
    PRIMARY KEY(id)
);
