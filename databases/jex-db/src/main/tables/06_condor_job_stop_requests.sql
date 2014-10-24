SET search_path = public, pg_catalog;

--
-- condor_job_stop_requests
--
CREATE TABLE condor_job_stop_requests (
  id             uuid not null default uuid_generate_v1(),
  job_id         uuid not null, -- foreign key into jobs table
  username       character varying(512) not null,
  date_requested timestamp with time zone not null,
  reason         text
);
