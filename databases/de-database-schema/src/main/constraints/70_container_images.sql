SET search_path = public, pg_catalog;

--
-- Primary key constraint for the container_images table.
--
ALTER TABLE ONLY container_images
    ADD CONSTRAINT container_images_pkey
    PRIMARY KEY(id);
