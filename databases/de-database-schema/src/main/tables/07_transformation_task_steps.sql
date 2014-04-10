SET search_path = public, pg_catalog;

--
-- transformation_task_steps table
--
CREATE TABLE transformation_task_steps (
    app_id character varying(255) NOT NULL,
    transformation_step_id bigint NOT NULL,
    hid integer NOT NULL
);

