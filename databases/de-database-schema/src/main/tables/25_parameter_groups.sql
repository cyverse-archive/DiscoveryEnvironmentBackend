SET search_path = public, pg_catalog;

--
-- parameter_groups table
--
CREATE TABLE parameter_groups (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    task_id uuid NOT NULL,
    name character varying(255),
    description text,
    label character varying(255) NOT NULL,
    display_order int NOT NULL DEFAULT 0,
    is_visible boolean DEFAULT true
);

