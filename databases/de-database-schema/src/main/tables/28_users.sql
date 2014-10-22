SET search_path = public, pg_catalog;

--
-- users table
--
CREATE TABLE users (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    username character varying(512) NOT NULL
);

