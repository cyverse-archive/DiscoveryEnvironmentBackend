SET search_path = public, pg_catalog;

--
-- Updates columns in the existing user_preferences table.
-- cols to drop: user_id_v187
--
ALTER TABLE ONLY user_preferences RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY user_preferences ADD COLUMN user_id UUID;

