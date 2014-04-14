SET search_path = public, pg_catalog;

--
-- ID sequence for the parameter_groups table.
--
CREATE SEQUENCE parameter_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- parameter_groups table
--
CREATE TABLE parameter_groups (
    hid bigint DEFAULT nextval('parameter_groups_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    task_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    group_type character varying(255),
    is_visible boolean
);
