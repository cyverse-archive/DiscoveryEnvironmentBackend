SET search_path = public, pg_catalog;

--
-- The join table for metadata templates and attributes.
--
CREATE TABLE attr_enum_values (
    id uuid NOT NULL DEFAULT (uuid_generate_v1()),
    attribute_id uuid NOT NULL,
    value text NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    display_order integer NOT NULL DEFAULT 0
);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX attr_enum_values_attribute_id
ON attr_enum_values(attribute_id);
