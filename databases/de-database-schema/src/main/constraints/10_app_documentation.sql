SET search_path = public, pg_catalog;

--
-- app_documentation table primary key.
--
ALTER TABLE app_documentation
ADD CONSTRAINT app_documentation_pkey
PRIMARY KEY (app_id);

--
-- app_documentation table foreign key.
--
ALTER TABLE ONLY app_documentation
ADD CONSTRAINT app_documentation_app_id_fkey
FOREIGN KEY (app_id)
REFERENCES apps(id) ON DELETE CASCADE;
