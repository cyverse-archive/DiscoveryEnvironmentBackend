SET search_path = public, pg_catalog;

--
-- Records individual steps of jobs that the user has submitted.
--
CREATE TABLE job_steps (
    job_id uuid NOT NULL,
    step_number integer NOT NULL,
    external_id character varying(40) NOT NULL,
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    job_type_id bigint NOT NULL,
    app_step_number integer NOT NULL
);
