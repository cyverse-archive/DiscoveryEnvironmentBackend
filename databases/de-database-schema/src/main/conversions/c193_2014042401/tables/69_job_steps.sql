SET search_path = public, pg_catalog;

--
-- Updates columns in the existing job_steps table.
--
ALTER TABLE ONLY job_steps RENAME COLUMN job_type_id TO job_type_id_v192;
ALTER TABLE ONLY job_steps ALTER COLUMN job_type_id_v192 DROP NOT NULL;
ALTER TABLE ONLY job_steps ADD COLUMN job_type_id UUID;

