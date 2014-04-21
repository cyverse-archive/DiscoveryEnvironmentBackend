SET search_path = public, pg_catalog;

---
--- A table for storing a user's saved searches
---
CREATE TABLE user_saved_searches (
    id UUID UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
    saved_searches JSON NOT NULL
);

---
--- Creates an index on the id column
---
CREATE INDEX user_saved_searches_id
ON user_saved_searches(id);

---
--- Creates an index on the user_id column
---
CREATE INDEX user_saved_searches_user_id
ON user_saved_searches(user_id);
