SET search_path = public, pg_catalog;

---
--- A table for storing a user's saved searches
---
CREATE TABLE user_saved_searches (
    id UUID,
    user_id UUID,
    saved_searches TEXT NOT NULL
);
