SET search_path = public, pg_catalog;

--
-- parameter_types table
--
CREATE TABLE parameter_types (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    deprecated boolean DEFAULT false,
    hidable boolean DEFAULT false,
    display_order integer DEFAULT 999,
    value_type_id uuid
);
