SET search_path = public, pg_catalog;

--
-- A table of synonyms for metadata attributes.
--
CREATE TABLE attr_synonyms (
    attribute_id uuid NOT NULL,
    synonym_id uuid NOT NULL
);

--
-- Creates an index on the attribute_id column.
--
CREATE INDEX attr_synonyms_attribute_id
ON attr_synonyms(attribute_id);

--
-- Creates an index on the synonym_id column.
--
CREATE INDEX attr_synonyms_synonym_id
ON attr_synonyms(synonym_id);
