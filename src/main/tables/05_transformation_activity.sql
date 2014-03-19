SET search_path = public, pg_catalog;

--
-- ID sequence for the transformation_activity.
--
CREATE SEQUENCE transformation_activity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- transformation_activity table
--
CREATE TABLE transformation_activity (
    hid bigint DEFAULT nextval('transformation_activity_id_seq'::regclass) NOT NULL,
    name character varying(255),
    id character varying(255) NOT NULL,
    description character varying(255),
    workspace_id bigint NOT NULL,
    type character varying(255),
    deleted boolean,
    integration_data_id bigint NOT NULL,
    wikiurl character varying(1024),
    integration_date timestamp without time zone,
    disabled boolean DEFAULT false NOT NULL,
    edited_date timestamp without time zone
);
