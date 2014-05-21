SET search_path = public, pg_catalog;

--
-- Updates job_types uuid foreign keys.
--
UPDATE jobs SET job_type_id =
    (SELECT t.id FROM job_types t
     WHERE t.id_v187 = job_type_id_v187);

