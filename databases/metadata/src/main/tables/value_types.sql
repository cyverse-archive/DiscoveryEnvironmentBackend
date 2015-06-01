SET search_path = public, pg_catalog;

--
-- Stores known metadata value types.
--
CREATE TABLE value_types (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name varchar(64) NOT NULL
);
