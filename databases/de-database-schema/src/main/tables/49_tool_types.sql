SET search_path = public, pg_catalog;


--
-- A table listing the types of tools that are currently available.
--
CREATE TABLE tool_types (
    id character varying(255) NOT NULL,
    name varchar(50) UNIQUE NOT NULL,
    label varchar(128) NOT NULL,
    description varchar(256),
    PRIMARY KEY(id)
);
