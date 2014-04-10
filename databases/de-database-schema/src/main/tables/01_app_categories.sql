SET search_path = public, pg_catalog;

--
-- app_categories table
--
CREATE TABLE app_categories (
    id character varying(255),
    name character varying(255),
    description character varying(255),
    workspace_id bigint
);
