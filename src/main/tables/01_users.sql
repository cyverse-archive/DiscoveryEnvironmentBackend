--
-- ID sequence for the users table.
--
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores user information.
--
CREATE TABLE users (
    id BIGINT DEFAULT nextval('users_id_seq'::regclass) NOT NULL,
    username VARCHAR(512) UNIQUE NOT NULL,
    PRIMARY KEY(id)
);
