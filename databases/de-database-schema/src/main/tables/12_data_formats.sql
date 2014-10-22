SET search_path = public, pg_catalog;

--
-- A table for data object file formats.
--
CREATE TABLE data_formats (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(64) NOT NULL,
    label character varying(255),
    display_order integer DEFAULT 999
);

