SET search_path = public, pg_catalog;

--
-- The statuses that have been applied to each tool request.
--
CREATE TABLE tool_request_statuses (
    id UUID NOT NULL DEFAULT uuid_generate_v4(),
    tool_request_id UUID NOT NULL,
    tool_request_status_code_id UUID NOT NULL,
    date_assigned TIMESTAMP DEFAULT now() NOT NULL,
    updater_id UUID NOT NULL,
    comments TEXT
);

