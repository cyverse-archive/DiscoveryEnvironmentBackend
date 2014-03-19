SET search_path = public, pg_catalog;

--
-- Stores known metadata field information.
--
CREATE TABLE metadata_attributes (
    id uuid NOT NULL,
    name varchar(64) NOT NULL,
    description varchar(1024) NOT NULL,
    required boolean NOT NULL,
    value_type_id uuid NOT NULL REFERENCES metadata_value_types(id),
    PRIMARY KEY (id)
);
