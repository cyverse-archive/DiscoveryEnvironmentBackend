--
-- Primary key for the jobs table
--
ALTER TABLE ONLY jobs
    ADD CONSTRAINT jobs_pkey
    PRIMARY KEY (id);


--
-- Primary key for the condor_events table
--
ALTER TABLE ONLY condor_events
    ADD CONSTRAINT condor_events_pkey
    PRIMARY KEY (id);


--
-- Primary key for the condor_raw_events table
--
ALTER TABLE ONLY condor_raw_events
    ADD CONSTRAINT condor_raw_events_pkey
    PRIMARY KEY (id);


--
-- Foreign key into the jobs table from condor_raw_events
--
ALTER TABLE ONLY condor_raw_events
    ADD CONSTRAINT condor_raw_events_job_id_fkey
    FOREIGN KEY (job_id)
    REFERENCES jobs(id) ON DELETE CASCADE;


--
-- Primary key for the condor_job_events table
--
ALTER TABLE ONLY condor_job_events
    ADD CONSTRAINT condor_job_events_pkey
    PRIMARY KEY (id);

--
-- Foreign key into the jobs table from condor_job_events
--
ALTER TABLE ONLY condor_job_events
    ADD CONSTRAINT condor_job_events_job_id_fkey
    FOREIGN KEY (job_id)
    REFERENCES jobs(id) ON DELETE CASCADE;

--
-- Foreign key into the condor_event table from condor_job_events
--
ALTER TABLE ONLY condor_job_events
    ADD CONSTRAINT condor_job_events_condor_event_id_fkey
    FOREIGN KEY (condor_event_id)
    REFERENCES condor_events(id) ON DELETE CASCADE;


--
-- Foreign key into the condor_event table from condor_job_events
--
ALTER TABLE ONLY condor_job_events
    ADD CONSTRAINT condor_job_events_condor_raw_event_id_fkey
    FOREIGN KEY (condor_raw_event_id)
    REFERENCES condor_raw_events(id) ON DELETE CASCADE;


--
-- Primary key for the last_condor_job_event table
--
ALTER TABLE ONLY last_condor_job_events
    ADD CONSTRAINT last_condor_job_events_pkey
    PRIMARY KEY (job_id);


--
-- Foreign key into the jobs table for last_condor_job_events
--
ALTER TABLE ONLY last_condor_job_events
    ADD CONSTRAINT last_condor_job_events_job_id_fkey
    FOREIGN KEY (job_id)
    REFERENCES jobs(id) ON DELETE CASCADE;


--
-- Foreign key into the condor_job_events table for last_condor_job_events
--
ALTER TABLE ONLY last_condor_job_events
    ADD CONSTRAINT last_condor_job_events_condor_job_event_id_fkey
    FOREIGN KEY (condor_job_event_id)
    REFERENCES condor_job_events(id) ON DELETE CASCADE;


--
-- Primary key for the condor_job_stop_requests table
--
ALTER TABLE ONLY condor_job_stop_requests
    ADD CONSTRAINT condor_job_stop_requests_pkey
    PRIMARY KEY (id);


--
-- Foreign key into the jobs table for condor_job_stop_requests
--
ALTER TABLE ONLY condor_job_stop_requests
    ADD CONSTRAINT condor_job_stop_requests_job_id_fkey
    FOREIGN KEY (job_id)
    REFERENCES jobs(id) ON DELETE CASCADE;


--
-- Primary key for condor_job_deps
--
ALTER TABLE ONLY condor_job_deps
    ADD CONSTRAINT condor_job_deps_pkey
    PRIMARY KEY (successor_id);


--
-- Foreign key into jobs for condor_job_deps
--
ALTER TABLE ONLY condor_job_deps
    ADD CONSTRAINT condor_job_deps_successor_id_fkey
    FOREIGN KEY (successor_id)
    REFERENCES jobs(id) ON DELETE CASCADE;


--
-- Foreign key into jobs for condor_job_deps
--
ALTER TABLE ONLY condor_job_deps
    ADD CONSTRAINT condor_job_deps_predecessor_id_fkey
    FOREIGN KEY (predecessor_id)
    REFERENCES jobs(id) ON DELETE CASCADE;


--
-- Primary key for the version table
--
ALTER TABLE ONLY version
    ADD CONSTRAINT version_pkey
    PRIMARY KEY (id);

--
-- Uniqueness constraint for the condor_id column of the jobs table
--
ALTER TABLE ONLY jobs
    ADD CONSTRAINT condor_id_key
    UNIQUE (condor_id);
