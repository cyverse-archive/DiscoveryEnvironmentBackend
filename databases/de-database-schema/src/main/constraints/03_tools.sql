SET search_path = public, pg_catalog;

--
-- Other constraints for this table are located in the 99_constraints.sql file.
--

-- Foreign key into the container_images table from the tools table.
ALTER TABLE ONLY tools
    ADD CONSTRAINT tools_container_image_fkey
    FOREIGN KEY(container_images_id)
    REFERENCES container_images(id);
