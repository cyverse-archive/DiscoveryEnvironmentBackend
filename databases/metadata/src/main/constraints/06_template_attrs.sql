SET search_path = public, pg_catalog;

--
-- Foreign key constraint for the template_id field of the template_attrs table.
--
ALTER TABLE ONLY template_attrs
ADD CONSTRAINT template_attrs_template_id_fkey
FOREIGN KEY (template_id)
REFERENCES templates(id);

--
-- Foreign key constraint for the attribute_id field of the template_attrs table.
--
ALTER TABLE ONLY template_attrs
ADD CONSTRAINT template_attrs_attribute_id_fkey
FOREIGN KEY (attribute_id)
REFERENCES attributes(id);
