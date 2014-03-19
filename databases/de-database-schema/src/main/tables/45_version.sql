SET search_path = public, pg_catalog;

--
-- The ID sequence used for database version records.
--
CREATE SEQUENCE version_id_seq;

--
-- A table to database versions along with the date they were applied.
--
CREATE TABLE version (
    id bigint DEFAULT nextval('version_id_seq'),
    version character varying(20) NOT NULL,
    applied timestamp DEFAULT now(),
    PRIMARY KEY (id)
);
