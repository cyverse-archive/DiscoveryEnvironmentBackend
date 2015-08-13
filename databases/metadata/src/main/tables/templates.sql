SET search_path = public, pg_catalog;

--
-- Stores a list of metadata templates.
--
CREATE TABLE templates (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name varchar(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL,
    created_by varchar(512) NOT NULL,
    modified_by varchar(512) NOT NULL,
    created_on timestamp DEFAULT now() NOT NULL,
    modified_on timestamp DEFAULT now() NOT NULL
);
