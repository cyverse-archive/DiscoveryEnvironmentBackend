SET search_path = public, pg_catalog;

--
-- app_documentation table primary key.
--
ALTER TABLE app_documentation
ADD CONSTRAINT app_documentation_pkey
PRIMARY KEY (app_id);
