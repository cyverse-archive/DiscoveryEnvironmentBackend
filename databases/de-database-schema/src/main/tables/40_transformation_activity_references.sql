SET search_path = public, pg_catalog;

--
-- id SERIAL type for transformation_activity_references table
--
CREATE SEQUENCE transformation_activity_references_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- transformation_activity_references table
--
CREATE TABLE transformation_activity_references (
    id bigint DEFAULT nextval('transformation_activity_references_id_seq'::regclass) NOT NULL,
    transformation_activity_id bigint NOT NULL,
    reference_text text NOT NULL
);
