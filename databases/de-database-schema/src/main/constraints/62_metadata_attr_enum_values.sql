SET search_path = public, pg_catalog;

--
-- Foreign key constraint for the attribute_id field of the metadata_attr_enum_values table.
--
ALTER TABLE ONLY metadata_attr_enum_values
ADD CONSTRAINT metadata_attr_enum_values_attribute_id_fkey
FOREIGN KEY (attribute_id)
REFERENCES metadata_attributes(id) ON DELETE CASCADE;
