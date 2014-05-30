SET search_path = public, pg_catalog;

--
-- tasks table
--
CREATE TABLE tasks (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    tool_id uuid
);
