SET search_path = public, pg_catalog;

--
-- tags table primary key.
--
ALTER TABLE tags
    ADD CONSTRAINT tags_pkey
    PRIMARY KEY (id);

--
-- tags table foreign key to the targets table.
--
ALTER TABLE tags
    ADD CONSTRAINT tags_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

