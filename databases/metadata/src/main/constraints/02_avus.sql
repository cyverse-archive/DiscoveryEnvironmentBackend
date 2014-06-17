SET search_path = public, pg_catalog;

--
-- avus table primary key.
--
ALTER TABLE avus
    ADD CONSTRAINT avus_pkey
    PRIMARY KEY (id);

--
-- avus table unique values contraint.
--
ALTER TABLE avus
    ADD CONSTRAINT avus_unique
    UNIQUE (owner_id, target_id, target_type, attribute, value, unit);

