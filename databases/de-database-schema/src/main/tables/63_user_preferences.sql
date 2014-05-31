SET search_path = public, pg_catalog;

--
-- A table containing user preferences.
--
CREATE TABLE user_preferences (
  id UUID NOT NULL DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL,
  preferences TEXT NOT NULL
);

