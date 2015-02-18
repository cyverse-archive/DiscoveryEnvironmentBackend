SET search_path = public, pg_catalog;

--
-- Primary key constraint for the container_settings table.
--
ALTER TABLE ONLY container_settings
    ADD CONSTRAINT container_settings_pkey
    PRIMARY KEY(id);

ALTER TABLE ONLY container_settings
    ADD CONSTRAINT container_settings_tools_id_fkey
    FOREIGN KEY(tools_id)
    REFERENCES tools(id);
