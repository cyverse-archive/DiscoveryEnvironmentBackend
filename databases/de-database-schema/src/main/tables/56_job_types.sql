SET search_path = public, pg_catalog;

--
-- ID sequence for the job_types table.
--
CREATE SEQUENCE job_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores information about the types of jobs that the DE can submit.
--
CREATE TABLE job_types (
    id bigint DEFAULT nextval('job_types_id_seq'::regclass) NOT NULL,
    name character varying(36) NOT NULL,
    PRIMARY KEY (id)
);
