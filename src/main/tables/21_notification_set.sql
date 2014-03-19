SET search_path = public, pg_catalog;

--
-- notification_set ID sequence
--
CREATE SEQUENCE notification_set_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- notification_set table
--
CREATE TABLE notification_set (
    hid bigint DEFAULT nextval('notification_set_id_seq'::regclass) NOT NULL,
    id character varying(255),
    name character varying(255),
    template_id character varying(255)
);
