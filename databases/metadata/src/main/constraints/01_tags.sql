SET search_path = public, pg_catalog;

--
-- tags table primary key.
--
ALTER TABLE tags
    ADD CONSTRAINT tags_pkey
    PRIMARY KEY (id);

