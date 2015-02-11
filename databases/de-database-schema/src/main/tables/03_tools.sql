SET search_path = public, pg_catalog;

--
-- tools table
--
CREATE TABLE tools (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(255) NOT NULL,
    location character varying(255),
    tool_type_id uuid NOT NULL,
    description text,
    version character varying(255),
    attribution text,
    integration_data_id uuid NOT NULL,
    container_settings_id uuid
);
