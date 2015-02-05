SET search_path = public, pg_catalog;

--
-- app_documentation table primary key.
--
ALTER TABLE app_documentation
ADD CONSTRAINT app_documentation_pkey
PRIMARY KEY (app_id);

--
-- app_documentation table foreign keys.
--
ALTER TABLE ONLY app_documentation
ADD CONSTRAINT app_documentation_app_id_fkey
FOREIGN KEY (app_id)
REFERENCES apps(id) ON DELETE CASCADE;

ALTER TABLE ONLY app_documentation
ADD CONSTRAINT app_documentation_created_by_fkey
FOREIGN KEY (created_by)
REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ONLY app_documentation
ADD CONSTRAINT app_documentation_modified_by_fkey
FOREIGN KEY (modified_by)
REFERENCES users(id) ON DELETE CASCADE;
