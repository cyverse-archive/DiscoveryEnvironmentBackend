SET search_path = public, pg_catalog;

--
-- Stores known metadata value types.
--
CREATE TABLE metadata_value_types (
    id uuid NOT NULL,
    name varchar(64) NOT NULL
);
