SET search_path = public, pg_catalog;

--
-- Updates columns in the existing access_tokens table.
--
ALTER TABLE ONLY access_tokens RENAME COLUMN user_id TO user_id_v192;
ALTER TABLE ONLY access_tokens ADD COLUMN user_id UUID;

