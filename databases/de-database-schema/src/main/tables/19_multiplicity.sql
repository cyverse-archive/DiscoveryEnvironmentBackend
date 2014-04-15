SET search_path = public, pg_catalog;

--
-- A table for our enumeration of multiplicities.
--
CREATE TABLE multiplicity (
    id uuid,
    name character varying(64),
    label character varying(255),
    description character varying(255),
    type_name character varying(64),
    output_type_name character varying(64)
);
