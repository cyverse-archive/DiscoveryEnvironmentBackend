SET search_path = public, pg_catalog;

--
-- favorites table
--
CREATE TABLE favorites (
  target_id UUID NOT NULL,
  target_type target_enum NOT NULL,
  owner_id varchar(512) NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL
);

