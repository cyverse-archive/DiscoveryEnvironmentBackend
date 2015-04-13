--
-- Foreign key constraint for the attribute_id field of the metadata_attr_synonyms table.
--
ALTER TABLE ONLY metadata_attr_synonyms
ADD CONSTRAINT metadata_attr_synonyms_attribute_id_fkey
FOREIGN KEY (attribute_id)
REFERENCES metadata_attributes(id) ON DELETE CASCADE;

--
-- Foreign key constraint for the synonym_id field of the metadata_attr_synonyms table.
--
ALTER TABLE ONLY metadata_attr_synonyms
ADD CONSTRAINT metadata_attr_synonyms_synonym_id_fkey
FOREIGN KEY (synonym_id)
REFERENCES metadata_attributes(id) ON DELETE CASCADE;
