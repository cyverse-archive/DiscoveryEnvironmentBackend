SET search_path = public, pg_catalog;

--
-- documentation table
--
CREATE TABLE documentation (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  value TEXT,
  target_id UUID NOT NULL,
  target_type target_enum NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL,
  created_by varchar(512) NOT NULL,
  modified_by varchar(512) NOT NULL
);
