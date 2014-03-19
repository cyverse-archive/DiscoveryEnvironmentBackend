SET search_path = public, pg_catalog;

--
-- The identifier sequence for the tool_requests table.
--
CREATE SEQUENCE tool_request_statuses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- The statuses that have been applied to each tool request.
--
CREATE TABLE tool_request_statuses (
    id BIGINT DEFAULT nextval('tool_request_statuses_id_seq'::regclass) NOT NULL,
    tool_request_id BIGINT REFERENCES tool_requests(id) NOT NULL,
    tool_request_status_code_id UUID REFERENCES tool_request_status_codes(id) NOT NULL,
    date_assigned TIMESTAMP DEFAULT now() NOT NULL,
    updater_id BIGINT NOT NULL,
    comments TEXT,
    PRIMARY KEY(id)
);
