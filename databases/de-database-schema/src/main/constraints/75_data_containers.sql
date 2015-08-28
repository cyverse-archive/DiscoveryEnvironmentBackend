SET search_path = public, pg_catalog;

--
-- Primary key constraint for the data_containers table.
--
ALTER TABLE ONLY data_containers
    ADD CONSTRAINT data_containers_pkey
    PRIMARY KEY (id);

-- Foreign key constraint for the data_containers.container_image_id field.
--
ALTER TABLE ONLY data_containers
    ADD CONSTRAINT data_containers_container_image_id_fkey
    FOREIGN KEY (container_image_id)
    REFERENCES container_images(id);
