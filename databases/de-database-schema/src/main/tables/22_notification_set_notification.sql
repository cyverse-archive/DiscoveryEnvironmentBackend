SET search_path = public, pg_catalog;

--
-- notification_set_notification table
--
CREATE TABLE notification_set_notification (
    notification_set_id bigint NOT NULL,
    notification_id bigint NOT NULL,
    hid integer NOT NULL
);
