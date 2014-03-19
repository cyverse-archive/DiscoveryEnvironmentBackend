--
-- ID sequence for the system_notifications table.
--
CREATE SEQUENCE system_notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores system notification information.
--
CREATE TABLE system_notifications (
    id BIGINT DEFAULT nextval('system_notifications_id_seq'::regclass) NOT NULL,
    uuid UUID NOT NULL,
    system_notification_type_id BIGINT REFERENCES system_notification_types(id) NOT NULL,
    date_created TIMESTAMP DEFAULT now() NOT NULL,
    activation_date TIMESTAMP DEFAULT now() NOT NULL,
    deactivation_date TIMESTAMP,
    dismissible BOOLEAN DEFAULT FALSE NOT NULL,
    logins_disabled BOOLEAN DEFAULT FALSE NOT NULL,
    message TEXT,
    PRIMARY KEY(id)
);
