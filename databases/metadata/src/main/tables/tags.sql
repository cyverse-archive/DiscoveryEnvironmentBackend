SET search_path = public, pg_catalog;

--
-- tags table
--
CREATE TABLE tags (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  value VARCHAR(255) NOT NULL,
  description TEXT,
  public BOOLEAN NOT NULL DEFAULT false,
  owner_id varchar(512),
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL
);

CREATE INDEX tags_owner_id_idx ON tags(owner_id);