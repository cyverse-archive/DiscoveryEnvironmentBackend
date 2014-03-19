--
-- ID sequence for the analysis_execution_statuses table.
--
CREATE SEQUENCE analysis_execution_statuses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores the last status seen by the notification agent for each job.  This
-- allows the notification agent to avoid spamming users with multiple
-- notifications with the same status for the same job.
--
CREATE TABLE analysis_execution_statuses (
    id BIGINT DEFAULT nextval('analysis_execution_statuses_id_seq'::regclass) NOT NULL,
    uuid UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    date_modified TIMESTAMP DEFAULT now() NOT NULL,
    PRIMARY KEY(id)
);
