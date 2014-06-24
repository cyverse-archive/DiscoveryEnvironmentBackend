SET search_path = public, pg_catalog;

--
-- Data source
--
CREATE TABLE data_source (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name varchar(50) NOT NULL,
    label varchar(50) NOT NULL,
    description varchar(255) NOT NULL
);

