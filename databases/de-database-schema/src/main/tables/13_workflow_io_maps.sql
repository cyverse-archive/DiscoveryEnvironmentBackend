SET search_path = public, pg_catalog;

--
-- workflow_io_maps table
--
CREATE TABLE workflow_io_maps (
    app_id character varying(255) NOT NULL,
    target_step character varying(255) NOT NULL,
    source_step character varying(255) NOT NULL,
    input character varying(255) NOT NULL,
    output character varying(255) NOT NULL
);
