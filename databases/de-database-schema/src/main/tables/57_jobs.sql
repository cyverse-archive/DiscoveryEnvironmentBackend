SET search_path = public, pg_catalog;

--
-- Records jobs that the user has submitted.
--
CREATE TABLE jobs (
    id uuid NOT NULL,
    external_id character varying(40) NOT NULL,
    job_name character varying(255) NOT NULL,
    app_name character varying(255),
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL,
    job_type_id bigint REFERENCES job_types(id) NOT NULL,
    user_id bigint NOT NULL,
    PRIMARY KEY (id)
);
