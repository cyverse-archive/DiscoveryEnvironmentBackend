SET search_path = public, pg_catalog;

--
-- Updates columns in the existing authorization_requests table.
--
ALTER TABLE ONLY authorization_requests RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY authorization_requests ADD COLUMN user_id UUID;

