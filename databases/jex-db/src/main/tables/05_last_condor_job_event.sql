SET search_path = public, pg_catalog;

--
-- last_condor_job_event
--
CREATE TABLE last_condor_job_events (
  job_id uuid not null, -- foreign key into the jobs table
  condor_job_event_id uuid not null -- foreign key into the job_events table
);
