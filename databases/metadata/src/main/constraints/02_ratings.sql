SET search_path = public, pg_catalog;

--
-- ratings table primary key.
--
ALTER TABLE ratings
    ADD CONSTRAINT ratings_pkey
    PRIMARY KEY (id);
