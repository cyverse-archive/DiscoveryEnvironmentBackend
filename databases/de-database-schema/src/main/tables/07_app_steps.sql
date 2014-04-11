SET search_path = public, pg_catalog;

--
-- app_steps table
--
CREATE TABLE app_steps (
    id character varying(255) NOT NULL,
    app_id character varying(255) NOT NULL,
    task_id character varying(255) NOT NULL,
    step integer NOT NULL
);
