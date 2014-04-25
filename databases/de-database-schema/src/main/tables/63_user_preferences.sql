SET search_path = public, pg_catalog;

--
-- A table containing user preferences.
--
CREATE TABLE user_preferences (
  id UUID,
  user_id BIGINT,
  preferences TEXT NOT NULL
);
