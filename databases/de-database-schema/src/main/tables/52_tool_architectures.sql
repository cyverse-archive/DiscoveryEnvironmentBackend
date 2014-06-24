SET search_path = public, pg_catalog;

--
-- A table for information about the systems on which a tool can run.
--
CREATE TABLE tool_architectures (
    id UUID NOT NULL DEFAULT uuid_generate_v1(),
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256) NOT NULL
);

--
-- All tool architecture names should be unique.
--
CREATE UNIQUE INDEX tool_architectures_name_index
    ON tool_architectures (name);

