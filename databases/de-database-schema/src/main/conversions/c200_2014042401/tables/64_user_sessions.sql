SET search_path = public, pg_catalog;

--
-- Updates columns in the existing user_sessions table.
--
ALTER TABLE ONLY user_sessions ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY user_sessions RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY user_sessions ADD COLUMN user_id UUID;

