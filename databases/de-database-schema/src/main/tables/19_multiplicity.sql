SET search_path = public, pg_catalog;

--
-- A table for our enumeration of multiplicities.
--
CREATE TABLE multiplicity (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(64),
    label character varying(255),
    description text,
    type_name character varying(64),
    output_type_name character varying(64)
);

