SET search_path = public, pg_catalog;

--
-- file_links table primary key.
--
ALTER TABLE file_links
    ADD CONSTRAINT file_links_pkey
    PRIMARY KEY (file_id, target_id, owner_id);

