SET search_path = public, pg_catalog;

---
--- A table containing user sessions
---
CREATE TABLE user_sessions (
    id UUID UNIQUE NOT NULL,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id);
    session JSON NOT NULL,
);

---
--- Creates an index on the id column
---
CREATE INDEX user_sessions_id
ON user_sessions(id);

---
--- Creates an index on the user_id column
---
CREATE INDEX user_session_user_id
ON user_sessions(user_id);
