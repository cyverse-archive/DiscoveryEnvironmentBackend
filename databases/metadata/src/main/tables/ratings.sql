SET search_path = public, pg_catalog;

--
-- ratings table
--
CREATE TABLE ratings (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  rating integer NOT NULL,
  target_id UUID NOT NULL,
  owner_id varchar(512) NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL
);

CREATE INDEX ratings_target_id_idx ON ratings(target_id);
CREATE INDEX ratings_owner_id_idx ON ratings(owner_id);

