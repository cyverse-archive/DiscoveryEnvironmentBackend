SET search_path = public, pg_catalog;

--
-- Records jobs that the user has submitted.
--
CREATE TABLE jobs (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    external_id character varying(40) NOT NULL,
    job_name character varying(255) NOT NULL,
    job_description text DEFAULT '',
    app_name character varying(255),
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL,
    job_type_id uuid NOT NULL,
    user_id uuid NOT NULL
);

