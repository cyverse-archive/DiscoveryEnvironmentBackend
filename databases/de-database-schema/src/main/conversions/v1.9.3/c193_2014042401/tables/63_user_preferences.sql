SET search_path = public, pg_catalog;

--
-- Updates columns in the existing user_preferences table.
--
ALTER TABLE ONLY user_preferences ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY user_preferences RENAME COLUMN user_id TO user_id_v192;
ALTER TABLE ONLY user_preferences ALTER COLUMN user_id_v192 DROP NOT NULL;
ALTER TABLE ONLY user_preferences ADD COLUMN user_id UUID;

