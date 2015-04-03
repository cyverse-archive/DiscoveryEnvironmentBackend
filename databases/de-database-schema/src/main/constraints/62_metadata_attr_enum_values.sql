SET search_path = public, pg_catalog;

--
-- metadata_attr_enum_values table primary key.
--
ALTER TABLE metadata_attr_enum_values
ADD CONSTRAINT metadata_attr_enum_values_pkey
PRIMARY KEY (id);

--
-- Foreign key constraint for the attribute_id field of the metadata_attr_enum_values table.
--
ALTER TABLE ONLY metadata_attr_enum_values
ADD CONSTRAINT metadata_attr_enum_values_attribute_id_fkey
FOREIGN KEY (attribute_id)
REFERENCES metadata_attributes(id) ON DELETE CASCADE;

--
-- Add a uniqueness constraint for each attribute's enum value.
--
ALTER TABLE ONLY metadata_attr_enum_values
ADD CONSTRAINT metadata_attr_enum_values_unique
UNIQUE(attribute_id, value);
