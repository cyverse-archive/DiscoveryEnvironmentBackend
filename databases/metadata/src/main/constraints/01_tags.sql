SET search_path = public, pg_catalog;

--
-- tags table primary key.
--
ALTER TABLE tags
    ADD CONSTRAINT tags_pkey
    PRIMARY KEY (id);

--
-- values have to be unique per user
--
ALTER TABLE tags
    ADD CONSTRAINT tags_unique_value_user
    UNIQUE (value, owner_id);
