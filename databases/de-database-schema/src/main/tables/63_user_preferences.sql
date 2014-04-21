SET search_path = public, pg_catalog;

--
-- A table containing user preferences.
--
CREATE TABLE user_preferences (
  id UUID UNIQUE NOT NULL,
  user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
  preferences TEXT NOT NULL,
  PRIMARY KEY (id)
);

---
--- Creates an index on the id column.
---
CREATE INDEX user_preferences_id
ON user_preferences(id);

---
--- Creates an index on the user_id column.
---
CREATE INDEX user_preferences_user_id
ON user_preferences(user_id);
