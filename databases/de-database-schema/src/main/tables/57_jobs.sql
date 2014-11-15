SET search_path = public, pg_catalog;

--
-- Records jobs that the user has submitted.
--
CREATE TABLE jobs (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    job_name character varying(255) NOT NULL,
    job_description text DEFAULT '',
    app_name character varying(255),
    app_id character varying(255),
    app_wiki_url text,
    app_description text,
    result_folder_path text,
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL,
    user_id uuid NOT NULL,
    submission json,
    parent_id uuid
);
