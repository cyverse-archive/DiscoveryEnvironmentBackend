SET search_path = public, pg_catalog;

--
-- jobs table
--
CREATE TABLE jobs (
  id                uuid not null default uuid_generate_v1(), -- primary key
  batch_id          uuid, -- self-join foreign key
  condor_id         character varying(32) not null,
  submitter         character varying(512) not null,
  invocation_id     uuid,
  date_submitted    timestamp with time zone,
  date_started      timestamp with time zone,
  date_completed    timestamp with time zone,
  app_id            uuid,
  exit_code         integer, -- nullable because the job might be running
  failure_threshold integer NOT NULL,
  failure_count     integer
);
