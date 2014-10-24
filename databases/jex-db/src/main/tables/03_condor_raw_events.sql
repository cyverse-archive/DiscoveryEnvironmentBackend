SET search_path = public, pg_catalog;

--
-- condor_raw_events
--
CREATE TABLE condor_raw_events (
  id             uuid not null default uuid_generate_v1(), -- primary key
  job_id         uuid not null, -- foreign key into the jobs table
  event_text     text not null,
  date_triggered timestamp with time zone not null
);
