SET search_path = public, pg_catalog;

--
-- tasks table
--
CREATE TABLE tasks (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    external_app_id character varying(255),
    name character varying(255) NOT NULL,
    description text,
    label character varying(255),
    tool_id uuid
);

