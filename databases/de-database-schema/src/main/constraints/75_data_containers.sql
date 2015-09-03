SET search_path = public, pg_catalog;

--
-- Primary key constraint for the data_containers table.
--
ALTER TABLE ONLY data_containers
    ADD CONSTRAINT data_containers_pkey
    PRIMARY KEY (id);

-- Foreign key constraint for the data_containers.container_images_id field.
--
ALTER TABLE ONLY data_containers
    ADD CONSTRAINT data_containers_container_images_id_fkey
    FOREIGN KEY (container_images_id)
    REFERENCES container_images(id);

--
-- Don't allow a data container to get listed with the same container_images_id more than once.
--
ALTER TABLE ONLY data_containers
    ADD CONSTRAINT data_containers_unique
    UNIQUE(container_images_id, name_prefix, read_only);
