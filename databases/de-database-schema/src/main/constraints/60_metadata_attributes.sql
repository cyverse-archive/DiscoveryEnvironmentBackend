--
-- Primary Key for the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
ADD CONSTRAINT metadata_attributes_pkey
PRIMARY KEY (id);

--
-- Foreign key constraint for the value_type_id field of the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
ADD CONSTRAINT metadata_attributes_value_type_id_fkey
FOREIGN KEY (value_type_id)
REFERENCES metadata_value_types(id);

--
-- Foreign key constraint for the created_by field of the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
ADD CONSTRAINT metadata_attributes_created_by_fkey
FOREIGN KEY (created_by)
REFERENCES users(id);

--
-- Foreign key constraint for the modified_by field of the metadata_attributes table.
--
ALTER TABLE ONLY metadata_attributes
ADD CONSTRAINT metadata_attributes_modified_by_fkey
FOREIGN KEY (modified_by)
REFERENCES users(id);
