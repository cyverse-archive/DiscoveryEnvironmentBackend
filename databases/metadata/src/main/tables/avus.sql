SET search_path = public, pg_catalog;

--
-- avus table
--
CREATE TABLE avus (
  id UUID NOT NULL,
  attribute TEXT,
  value TEXT,
  unit TEXT,
  target_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL
);

