SET search_path = public, pg_catalog;

--
-- integration_data table
--
CREATE TABLE integration_data (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    integrator_name character varying(255) NOT NULL CHECK (integrator_name ~ '\S'),
    integrator_email character varying(255) NOT NULL CHECK (integrator_email ~ '\S')
);
