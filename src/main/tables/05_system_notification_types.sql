--
-- ID sequence for the system_notification_types table.
--
CREATE SEQUENCE system_notification_types_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores types of system notifications.
--
CREATE TABLE system_notification_types (
    id BIGINT DEFAULT nextval('system_notification_types_id_seq'::regclass) NOT NULL,
    name VARCHAR(32),
    PRIMARY KEY(id)
);
