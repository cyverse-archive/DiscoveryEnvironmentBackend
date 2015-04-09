--
-- Primary Key for the metadata_templates table.
--
ALTER TABLE ONLY metadata_templates
ADD CONSTRAINT metadata_templates_pkey
PRIMARY KEY (id);

--
-- Foreign key constraint for the created_by field of the metadata_templates table.
--
ALTER TABLE ONLY metadata_templates
ADD CONSTRAINT metadata_templates_created_by_fkey
FOREIGN KEY (created_by)
REFERENCES users(id);

--
-- Foreign key constraint for the modified_by field of the metadata_templates table.
--
ALTER TABLE ONLY metadata_templates
ADD CONSTRAINT metadata_templates_modified_by_fkey
FOREIGN KEY (modified_by)
REFERENCES users(id);
