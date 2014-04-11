SET search_path = public, pg_catalog;

--
-- template_input table
--
CREATE TABLE template_input (
    task_id character varying(255) NOT NULL,
    input_id bigint NOT NULL,
    hid integer NOT NULL
);
