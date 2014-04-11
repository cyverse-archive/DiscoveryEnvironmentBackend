SET search_path = public, pg_catalog;

--
-- template_output table
--
CREATE TABLE template_output (
    task_id character varying(255) NOT NULL,
    output_id bigint NOT NULL,
    hid integer NOT NULL
);
