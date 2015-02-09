SET search_path = public, pg_catalog;

--
-- Primary key constraint for the container_volumes table.
--
ALTER TABLE ONLY container_volumes
    ADD CONSTRAINT container_volumes_pkey
    PRIMARY KEY(id);

--
-- Foreign key constraint on the container_volumes table against the
-- container_settings table.
--
ALTER TABLE ONLY container_volumes
    ADD CONSTRAINT container_volumes_container_settings_id_fkey
    FOREIGN KEY(container_settings_id)
    REFERENCES container_settings(id);
