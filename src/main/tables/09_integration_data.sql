SET search_path = public, pg_catalog;

--
-- id SERIAL type for integration_data table
--
CREATE SEQUENCE integration_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- integration_data table
--
CREATE TABLE integration_data (
    id bigint DEFAULT nextval('integration_data_id_seq'::regclass) NOT NULL,
    integrator_name character varying(255),
    integrator_email character varying(255)
);


