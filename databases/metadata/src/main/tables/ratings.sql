SET search_path = public, pg_catalog;

--
-- ratings table
--
CREATE TABLE ratings (
  id UUID NOT NULL,
  rating integer NOT NULL,
  target_id UUID NOT NULL,
  owner_id UUID NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL
);

