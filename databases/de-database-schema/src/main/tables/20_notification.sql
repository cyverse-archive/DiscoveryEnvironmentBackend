SET search_path = public, pg_catalog;

--
-- ID sequence for the notification table.
--
CREATE SEQUENCE notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;

--
-- notification table
--
CREATE TABLE notification (
    hid bigint DEFAULT nextval('notification_id_seq'::regclass) NOT NULL,
    id character varying(255),
    name character varying(255),
    sender character varying(255),
    type character varying(255)
);
