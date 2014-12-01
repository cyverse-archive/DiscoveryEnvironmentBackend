SET search_path = public, pg_catalog;

--
-- Drops obsolete tables that have no data.
--
DROP TABLE notification;
DROP TABLE notification_set;
DROP TABLE notification_set_notification;
DROP TABLE notifications_receivers;

DROP SEQUENCE notification_id_seq;
DROP SEQUENCE notification_set_id_seq;

