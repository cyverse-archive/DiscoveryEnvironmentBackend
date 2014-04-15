SET search_path = public, pg_catalog;

--
-- Data source
--
CREATE TABLE data_source (
    id uuid NOT NULL,
    name varchar(50) NOT NULL,
    label varchar(50) NOT NULL,
    description varchar(255) NOT NULL
);
