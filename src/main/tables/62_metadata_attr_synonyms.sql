SET search_path = public, pg_catalog;

--
-- A table of synonyms for metadata attributes.
--
CREATE TABLE metadata_attr_synonyms (
    attribute_id uuid NOT NULL REFERENCES metadata_attributes(id),
    synonym_id uuid NOT NULL REFERENCES metadata_attributes(id)
);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX metadata_attr_synonyms_attribute_id
ON metadata_attr_synonyms(attribute_id);

--
-- Creates an index on the synonym_id column.
--
CREATE INDEX metadata_attr_synonyms_synonym_id
ON metadata_attr_synonyms(synonym_id);
