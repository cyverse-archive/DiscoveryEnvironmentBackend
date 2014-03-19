SET search_path = public, pg_catalog;

--
-- An ID counter for the data_source table
--
CREATE SEQUENCE data_source_id_seq;

--
-- Data source
--
CREATE TABLE data_source (
    id bigint DEFAULT nextval('data_source_id_seq'),
    uuid char(36) NOT NULL,
    name varchar(50) NOT NULL,
    label varchar(50) NOT NULL,
    description varchar(255) NOT NULL
);
