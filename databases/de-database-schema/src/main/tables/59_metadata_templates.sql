SET search_path = public, pg_catalog;

--
-- Stores a list of metadata templates.
--
CREATE TABLE metadata_templates (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    name varchar(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL
);

