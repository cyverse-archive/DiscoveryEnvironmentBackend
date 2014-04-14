SET search_path = public, pg_catalog;

--
-- A table for storing default parameter values and value options (list items).
--
CREATE TABLE parameter_values (
    id character varying(255) NOT NULL,
    parameter_id character varying(255) NOT NULL,
    parent_id character varying(255),
    is_default boolean DEFAULT false,
    name character varying(255) NOT NULL,
    value character varying(255) NOT NULL,
    description text,
    label text
);
