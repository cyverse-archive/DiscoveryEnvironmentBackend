SET search_path = public, pg_catalog;

--
-- Updates columns in the existing users table.
-- cols to drop: id_v187
--
ALTER TABLE ONLY users RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY users ADD COLUMN id UUID DEFAULT (uuid_generate_v4());

