SET search_path = public, pg_catalog;

--
-- The join table for metadata templates and attributes.
--
CREATE TABLE metadata_template_attrs (
    template_id uuid NOT NULL REFERENCES metadata_templates(id),
    attribute_id uuid NOT NULL REFERENCES metadata_attributes(id),
    display_order integer NOT NULL
);

--
-- Creates an index on the template_id column.
--
CREATE INDEX metadata_template_attrs_template_id
ON metadata_template_attrs(template_id);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX metadata_template_attrs_attribute_id
ON metadata_template_attrs(attribute_id);
