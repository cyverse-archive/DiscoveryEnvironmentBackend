SET search_path = public, pg_catalog;

--
-- ID sequence for the input_output_mapping table
--
CREATE SEQUENCE input_output_mapping_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- input_output_mapping table
--
CREATE TABLE input_output_mapping (
    hid bigint DEFAULT nextval('input_output_mapping_id_seq'::regclass) NOT NULL,
    source bigint NOT NULL,
    target bigint NOT NULL
);
