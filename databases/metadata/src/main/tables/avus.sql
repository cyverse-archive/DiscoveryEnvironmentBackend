SET search_path = public, pg_catalog;

--
-- avus table
--
CREATE TABLE avus (
  id UUID NOT NULL DEFAULT uuid_generate_v4(),
  attribute TEXT,
  value TEXT,
  unit TEXT,
  target_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL
);

CREATE INDEX avus_target_id_idx ON avus(target_id);
CREATE INDEX avus_owner_id_idx ON avus(owner_id);

