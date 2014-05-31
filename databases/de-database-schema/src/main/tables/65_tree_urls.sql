SET search_path = public, pg_catalog;

---
--- A table of tree urls associated with files.
---
CREATE TABLE tree_urls (
    id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    sha1 VARCHAR(40) UNIQUE NOT NULL,
    tree_urls TEXT NOT NULL
);

---
--- Creates an index on the id and sha1 columns
---
CREATE INDEX tree_urls_id
ON tree_urls(id);

CREATE INDEX tree_urls_sha1
ON tree_urls(sha1);
