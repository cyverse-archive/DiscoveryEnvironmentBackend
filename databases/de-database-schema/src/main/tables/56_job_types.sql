SET search_path = public, pg_catalog;

--
-- Stores information about the types of jobs that the DE can submit.
--
CREATE TABLE job_types (
    id uuid NOT NULL,
    name character varying(36) NOT NULL,
    PRIMARY KEY (id)
);
