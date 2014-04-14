SET search_path = public, pg_catalog;

--
-- parameters table
--
CREATE TABLE parameters (
    id character varying(255) NOT NULL,
    parameter_group_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    label text,
    defalut_value character varying(255),
    is_visible boolean,
    ordering integer,
    parameter_type character varying(255),
    required boolean DEFAULT false,
    dataobject_id bigint,
    omit_if_blank boolean DEFAULT true
);
