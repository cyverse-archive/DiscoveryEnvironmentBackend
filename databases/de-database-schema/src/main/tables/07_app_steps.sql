SET search_path = public, pg_catalog;

--
-- app_steps table
--
CREATE TABLE app_steps (
    app_id character varying(255) NOT NULL,
    transformation_step_id bigint NOT NULL,
    hid integer NOT NULL
);
