SET search_path = public, pg_catalog;

--
-- The join table for metadata templates and attributes.
--
CREATE TABLE metadata_attr_enum_values (
    attribute_id uuid NOT NULL,
    value text NOT NULL,
    display_order integer NOT NULL DEFAULT 0
);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX metadata_attr_enum_values_attribute_id
ON metadata_attr_enum_values(attribute_id);
