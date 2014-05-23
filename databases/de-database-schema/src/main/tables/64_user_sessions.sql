SET search_path = public, pg_catalog;

---
--- A table containing user sessions
---
CREATE TABLE user_sessions (
    id UUID,
    user_id UUID NOT NULL,
    session TEXT NOT NULL
);
