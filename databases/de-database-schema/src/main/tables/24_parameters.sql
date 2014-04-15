SET search_path = public, pg_catalog;

--
-- parameters table
--
CREATE TABLE parameters (
    id uuid NOT NULL,
    parameter_group_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label text,
    defalut_value character varying(255),
    is_visible boolean,
    ordering integer,
    parameter_type uuid,
    required boolean DEFAULT false,
    file_parameter_id uuid,
    omit_if_blank boolean DEFAULT true
);
