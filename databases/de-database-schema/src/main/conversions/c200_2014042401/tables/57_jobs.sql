SET search_path = public, pg_catalog;

--
-- Updates columns in the existing jobs table.
--
ALTER TABLE ONLY jobs RENAME COLUMN job_type_id TO job_type_id_v187;
ALTER TABLE ONLY jobs RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY jobs ADD COLUMN job_type_id UUID;
ALTER TABLE ONLY jobs ADD COLUMN user_id UUID;

