SET search_path = public, pg_catalog;

--
-- The identifier sequence for the tool_architectures table.
--
CREATE SEQUENCE tool_architectures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- A table for information about the systems on which a tool can run.
--
CREATE TABLE tool_architectures (
    id BIGINT DEFAULT nextval('tool_architectures_id_seq'::regclass) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256) NOT NULL,
    PRIMARY KEY(id)
);

--
-- All tool architecture names should be unique.
--
CREATE UNIQUE INDEX tool_architectures_name_index
    ON tool_architectures (name);
