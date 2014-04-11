SET search_path = public, pg_catalog;

--
-- ID sequence for the tasks table.
--
CREATE SEQUENCE tasks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- tasks table
--
CREATE TABLE tasks (
    hid bigint DEFAULT nextval('tasks_id_seq'::regclass) NOT NULL,
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    type character varying(255),
    component_id character varying(255)
);
