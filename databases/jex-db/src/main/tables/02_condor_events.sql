SET search_path = public, pg_catalog;

--
-- condor_events
--
CREATE TABLE condor_events (
  id           uuid not null default uuid_generate_v1(), -- primary key
  event_number integer not null,
  event_name   text not null,
  event_desc   text not null
);
