SET search_path = public, pg_catalog;

--
-- targets table
--
CREATE TABLE targets (
  id UUID NOT NULL,
  type target_enum NOT NULL
);
