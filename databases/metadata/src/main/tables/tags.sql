SET search_path = public, pg_catalog;

--
-- tags table
--
CREATE TABLE tags (
  id UUID NOT NULL DEFAULT uuid_generate_v4(),
  value VARCHAR(255),
  description TEXT,
  public BOOLEAN DEFAULT false,
  target_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL
);

CREATE INDEX tags_target_id_idx ON tags(target_id);
CREATE INDEX tags_owner_id_idx ON tags(owner_id);

