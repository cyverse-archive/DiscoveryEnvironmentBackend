SET search_path = public, pg_catalog;

--
-- id SERIAL type for users table
--
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- users table
--
CREATE TABLE users (
    id bigint DEFAULT nextval('users_id_seq'::regclass) UNIQUE NOT NULL,
    username character varying(512) NOT NULL
);
