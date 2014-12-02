SET search_path = public, pg_catalog;

--
-- Updates columns in the existing job_types table.
--
ALTER TABLE ONLY job_types RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY job_types ADD COLUMN id UUID DEFAULT (uuid_generate_v1());

