SET search_path = public, pg_catalog;

--
-- Foreign key constraint for the template_id field of the metadata_template_attrs table.
--
ALTER TABLE ONLY metadata_template_attrs
    ADD CONSTRAINT metadata_template_attrs_template_id_fkey
    FOREIGN KEY (template_id)
    REFERENCES metadata_templates(id);

--
-- Foreign key constraint for the attribute_id field of the metadata_template_attrs table.
--
ALTER TABLE ONLY metadata_template_attrs
    ADD CONSTRAINT metadata_template_attrs_attribute_id_fkey
    FOREIGN KEY (attribute_id)
    REFERENCES metadata_attributes(id);
