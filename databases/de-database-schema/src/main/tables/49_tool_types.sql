SET search_path = public, pg_catalog;

--
-- The identifier sequence for the tool type table.
--
CREATE SEQUENCE tool_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- A table listing the types of tools that are currently available.
--
CREATE TABLE tool_types (
    id bigint DEFAULT nextval('tool_types_id_seq'::regclass) NOT NULL,
    name varchar(50) UNIQUE NOT NULL,
    label varchar(128) NOT NULL,
    description varchar(256),
    PRIMARY KEY(id)
);
