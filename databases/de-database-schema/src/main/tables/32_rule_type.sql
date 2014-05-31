SET search_path = public, pg_catalog;

--
-- rule_type table
--
CREATE TABLE rule_type (
    id uuid NOT NULL DEFAULT uuid_generate_v4(),
    name character varying(255) NOT NULL,
    description character varying(255),
    label character varying(255),
    deprecated boolean DEFAULT false,
    display_order integer DEFAULT 999,
    rule_description_format character varying(255) DEFAULT '',
    rule_subtype_id uuid NOT NULL
);

