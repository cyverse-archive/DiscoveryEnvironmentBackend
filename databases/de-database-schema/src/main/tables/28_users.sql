SET search_path = public, pg_catalog;

--
-- users table
--
CREATE TABLE users (
    id uuid NOT NULL,
    username character varying(512) NOT NULL
);
