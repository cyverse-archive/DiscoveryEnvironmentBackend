SET search_path = public, pg_catalog;

--
-- file_links table primary key.
--
ALTER TABLE file_links
    ADD CONSTRAINT file_links_pkey
    PRIMARY KEY (file_id, target_id, owner_id);

--
-- file_links table foreign key to the targets table.
--
ALTER TABLE file_links
    ADD CONSTRAINT file_links_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

