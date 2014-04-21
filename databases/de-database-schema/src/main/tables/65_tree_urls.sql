SET search_path = public, pg_catalog;

---
--- A table of tree urls associated with files.
---
CREATE TABLE tree_urls (
    id UUID UNIQUE NOT NULL,
    tree_urls JSON NOT NULL
);

---
--- Creates an index on the id column
---
CREATE INDEX tree_urls_id
ON tree_urls(id);
