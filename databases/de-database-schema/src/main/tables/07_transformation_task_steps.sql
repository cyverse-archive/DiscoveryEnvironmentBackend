SET search_path = public, pg_catalog;

--
-- transformation_task_steps table
--
CREATE TABLE transformation_task_steps (
    transformation_task_id bigint NOT NULL,
    transformation_step_id bigint NOT NULL,
    hid integer NOT NULL
);

