SET search_path = public, pg_catalog;

--
-- Primary key constraint for the job_status_updates table
--
ALTER TABLE ONLY job_status_updates
    ADD CONSTRAINT job_status_updates_pkey
    PRIMARY KEY(id);
