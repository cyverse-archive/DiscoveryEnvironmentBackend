SET search_path = public, pg_catalog;

--
-- workflow_io_maps table
--
CREATE TABLE workflow_io_maps (
    id uuid NOT NULL DEFAULT (uuid_generate_v1()),
    app_id uuid NOT NULL,
    target_step uuid NOT NULL,
    source_step uuid NOT NULL
);
