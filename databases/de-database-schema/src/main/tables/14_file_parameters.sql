SET search_path = public, pg_catalog;

--
-- file_parameters table
--
CREATE TABLE file_parameters (
    id uuid,
    name character varying(255),
    label character varying(255),
    orderd integer,
    switch character varying(255),
    info_type uuid NOT NULL,
    data_format uuid NOT NULL,
    description character varying(255),
    required boolean DEFAULT true,
    multiplicity uuid NOT NULL,
    retain boolean DEFAULT false,
    is_implicit boolean DEFAULT false,
    data_source_id uuid
);
