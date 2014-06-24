SET search_path = public, pg_catalog;

--
-- Stores information about the types of jobs that the DE can submit.
--
CREATE TABLE job_types (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(36) NOT NULL
);

