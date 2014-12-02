SET search_path = public, pg_catalog;

--
-- Updates job_types uuid foreign keys.
--
UPDATE job_steps SET job_type_id =
    (SELECT t.id FROM job_types t
     WHERE t.id_v192 = job_type_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY job_steps ALTER COLUMN job_type_id SET NOT NULL;

