SET search_path = public, pg_catalog;

---
--- A table for storing a list of webapps that the DE is integrated with.
---
CREATE TABLE integrated_webapps (
    id UUID NOT NULL,
    name VARCHAR(64) NOT NULL
);
