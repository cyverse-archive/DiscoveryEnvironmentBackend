SET search_path = public, pg_catalog;

--
-- Primary key constraint for the container_devices table.
--
ALTER TABLE ONLY container_devices
    ADD CONSTRAINT container_devices_pkey
    PRIMARY KEY(id);

--
-- Foreign key constraint on the container_devices table against the
-- container_settings table.
--
ALTER TABLE ONLY container_devices
    ADD CONSTRAINT container_devices_container_settings_id_fkey
    FOREIGN KEY(container_settings_id)
    REFERENCES container_settings(id);
