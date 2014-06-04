SET search_path = public, pg_catalog;

--
-- ratings table primary key.
--
ALTER TABLE ratings
    ADD CONSTRAINT ratings_pkey
    PRIMARY KEY (id);

--
-- ratings table foreign key to the targets table.
--
ALTER TABLE ratings
    ADD CONSTRAINT ratings_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

