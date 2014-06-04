SET search_path = public, pg_catalog;

--
-- favorites table primary key.
--
ALTER TABLE favorites
    ADD CONSTRAINT favorites_pkey
    PRIMARY KEY (owner_id, target_id);

--
-- favorites table foreign key to the targets table.
--
ALTER TABLE favorites
    ADD CONSTRAINT favorites_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

