SET search_path = public, pg_catalog;

--
-- condor_job_deps
--
CREATE TABLE condor_job_deps (
  successor_id   uuid not null, -- foreign key into the jobs table
  predecessor_id uuid not null -- also a foreign key into the jobs table
);
