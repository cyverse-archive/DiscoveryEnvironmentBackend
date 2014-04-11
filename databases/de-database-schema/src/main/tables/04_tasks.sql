SET search_path = public, pg_catalog;

--
-- tasks table
--
CREATE TABLE tasks (
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    type character varying(255),
    component_id character varying(255)
);
