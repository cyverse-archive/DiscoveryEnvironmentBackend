SET search_path = public, pg_catalog;

--
-- The join table for metadata templates and attributes.
--
CREATE TABLE template_attrs (
    template_id uuid NOT NULL,
    attribute_id uuid NOT NULL,
    display_order integer NOT NULL
);

--
-- Creates an index on the template_id column.
--
CREATE INDEX template_attrs_template_id
ON template_attrs(template_id);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX template_attrs_attribute_id
ON template_attrs(attribute_id);
