SET search_path = public, pg_catalog;

--
-- id SERIAL type for transformation_steps table
--
CREATE SEQUENCE transformation_steps_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- transformation_steps table
--
CREATE TABLE transformation_steps (
    id bigint DEFAULT nextval('transformation_steps_id_seq'::regclass) NOT NULL,
    name character varying(255),
    guid character varying(255),
    description character varying(255),
    transformation_id bigint
);

