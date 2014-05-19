SET search_path = public, pg_catalog;

--
-- Updates columns in the existing logins table.
-- cols to drop: user_id_v187
--
ALTER TABLE ONLY logins RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY logins ADD COLUMN user_id UUID;

