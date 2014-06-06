SET search_path = public, pg_catalog;

--
-- avus table primary key.
--
ALTER TABLE avus
    ADD CONSTRAINT avus_pkey
    PRIMARY KEY (id);

--
-- avus table foreign key to the targets table.
--
ALTER TABLE avus
    ADD CONSTRAINT avus_target_id_fkey
    FOREIGN KEY (target_id)
    REFERENCES targets(id);

--
-- avus table unique values contraint.
--
ALTER TABLE avus
    ADD CONSTRAINT avus_unique
    UNIQUE (owner_id, target_id, attribute, value, unit);

