SET search_path = public, pg_catalog;

--
-- Updates columns in the existing job_types table.
-- cols to drop: id_v187
--
ALTER TABLE ONLY job_types RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY job_types ADD COLUMN id UUID DEFAULT (uuid_generate_v4());

