SET search_path = public, pg_catalog;

--
-- Updates columns in the existing user_saved_searches table.
-- cols to drop: user_id_v187
--
ALTER TABLE ONLY user_saved_searches RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY user_saved_searches ADD COLUMN user_id UUID;

