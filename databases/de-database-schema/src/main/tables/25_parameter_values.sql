SET search_path = public, pg_catalog;

--
-- A table for storing default parameter values and value options (list items).
--
CREATE TABLE parameter_values (
    id uuid NOT NULL DEFAULT (uuid_generate_v4()),
    parameter_id uuid NOT NULL,
    parent_id uuid,
    is_default boolean DEFAULT false,
    param_order int,
    name character varying(255),
    value character varying(255),
    description text,
    label text
);
