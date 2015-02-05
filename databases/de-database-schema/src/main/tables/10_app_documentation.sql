SET search_path = public, pg_catalog;

--
-- app_documentation table
--
CREATE TABLE app_documentation (
  app_id UUID NOT NULL,
  value TEXT,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL,
  created_by UUID NOT NULL,
  modified_by UUID NOT NULL
);
