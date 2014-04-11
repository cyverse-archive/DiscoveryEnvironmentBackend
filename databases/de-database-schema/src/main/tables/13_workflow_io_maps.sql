SET search_path = public, pg_catalog;

--
-- workflow_io_maps table
--
CREATE TABLE workflow_io_maps (
    mapping_id bigint NOT NULL,
    input character varying(255) NOT NULL,
    output character varying(255) NOT NULL
);
