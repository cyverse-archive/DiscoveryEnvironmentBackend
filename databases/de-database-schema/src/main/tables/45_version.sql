SET search_path = public, pg_catalog;

--
-- A table to database versions along with the date they were applied.
--
CREATE TABLE version (
    version character varying(20) NOT NULL,
    applied timestamp DEFAULT now(),
    PRIMARY KEY (version)
);
