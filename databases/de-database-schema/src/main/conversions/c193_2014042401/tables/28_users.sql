SET search_path = public, pg_catalog;

--
-- Updates columns in the existing users table.
--
ALTER TABLE ONLY users RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY users ADD COLUMN id UUID DEFAULT (uuid_generate_v1());

UPDATE users SET id = '00000000-0000-0000-0000-000000000000'
  WHERE username = '<public>';

