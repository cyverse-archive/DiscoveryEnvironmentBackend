SET search_path = public, pg_catalog;

--
-- favorites table primary key.
--
ALTER TABLE favorites
    ADD CONSTRAINT favorites_pkey
    PRIMARY KEY (owner_id, target_id);
