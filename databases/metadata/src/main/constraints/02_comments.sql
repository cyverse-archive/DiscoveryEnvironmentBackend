SET search_path = public, pg_catalog;

--
-- comments table primary key.
--
ALTER TABLE comments
    ADD CONSTRAINT comments_pkey
    PRIMARY KEY (id);

--
-- comments table foreign key to the targets table.
--
ALTER TABLE comments
    ADD CONSTRAINT comments_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

