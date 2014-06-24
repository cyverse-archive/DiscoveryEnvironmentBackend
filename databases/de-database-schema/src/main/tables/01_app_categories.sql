SET search_path = public, pg_catalog;

--
-- app_categories table
--
CREATE TABLE app_categories (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(255),
    description character varying(255),
    workspace_id uuid NOT NULL
);

