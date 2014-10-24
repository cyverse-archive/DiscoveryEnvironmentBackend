SET search_path = public, pg_catalog;

--
-- condor_job_events
--
CREATE TABLE condor_job_events(
  id                  uuid not null default uuid_generate_v1(), -- primary key
  job_id              uuid not null, -- foreign key into the jobs table
  condor_event_id     uuid not null, -- foreign key into the condor_events table
  condor_raw_event_id uuid not null, -- foreign key into the condor_raw_events table
  date_triggered      timestamp with time zone not null
);
