SET search_path = public, pg_catalog;

--
-- notifications_receivers table
--
CREATE TABLE notifications_receivers (
    notification_id bigint NOT NULL,
    receiver character varying(255),
    hid integer NOT NULL
);
