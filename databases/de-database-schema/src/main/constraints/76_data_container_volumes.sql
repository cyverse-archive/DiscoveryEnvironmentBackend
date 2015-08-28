SET search_path = public, pg_catalog;

--
-- Primary key constraint for the data_container_volumes table.
--
ALTER TABLE ONLY data_container_volumes
    ADD CONSTRAINT data_container_volumes_pkey
    PRIMARY KEY (id);

--
-- Foreign key constraint for the data_container_volumes.data_containers_id
-- field.
--
ALTER TABLE ONLY data_container_volumes
    ADD CONSTRAINT data_container_volumes_data_containers_id_fkey
    FOREIGN KEY (data_containers_id)
    REFERENCES data_containers(id);
