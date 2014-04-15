SET search_path = public, pg_catalog;

--
-- integration_data table
--
CREATE TABLE integration_data (
    id uuid NOT NULL,
    integrator_name character varying(255),
    integrator_email character varying(255)
);
