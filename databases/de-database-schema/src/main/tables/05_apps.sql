SET search_path = public, pg_catalog;

--
-- apps table
--
CREATE TABLE apps (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    name character varying(255),
    description character varying(255),
    deleted boolean DEFAULT false NOT NULL,
    disabled boolean DEFAULT false NOT NULL,
    integration_data_id uuid NOT NULL,
    wikiurl character varying(1024),
    integration_date timestamp without time zone,
    edited_date timestamp without time zone
);

