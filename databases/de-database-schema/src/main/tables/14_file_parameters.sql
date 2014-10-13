SET search_path = public, pg_catalog;

--
-- file_parameters table
--
CREATE TABLE file_parameters (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    parameter_id uuid,
    retain boolean DEFAULT false,
    is_implicit boolean DEFAULT false,
    info_type uuid NOT NULL,
    data_format uuid NOT NULL,
    data_source_id uuid NOT NULL
);
