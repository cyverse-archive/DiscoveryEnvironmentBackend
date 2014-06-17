SET search_path = public, pg_catalog;

--
-- comments table primary key.
--
ALTER TABLE comments
    ADD CONSTRAINT comments_pkey
    PRIMARY KEY (id);

