SET search_path = public, pg_catalog;

--
-- targets table primary key.
--
ALTER TABLE targets
    ADD CONSTRAINT targets_pkey
    PRIMARY KEY (id);
