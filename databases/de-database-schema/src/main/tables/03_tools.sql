SET search_path = public, pg_catalog;

--
-- tools table
--
CREATE TABLE tools (
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    location character varying(255),
    tool_type_id character varying(255) NOT NULL,
    description text,
    version character varying(255),
    attribution text,
    integration_data_id bigint NOT NULL
);
