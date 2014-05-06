SET search_path = public, pg_catalog;

---
--- A table containing user sessions
---
CREATE TABLE user_sessions (
    id UUID,
    user_id UUID,
    session TEXT NOT NULL
);
