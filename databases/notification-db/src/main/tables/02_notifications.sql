--
-- ID sequence for the notifications table.
--
CREATE SEQUENCE notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- Stores notification information.
--
CREATE TABLE notifications (
    id BIGINT DEFAULT nextval('notifications_id_seq'::regclass) NOT NULL,
    uuid UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    subject TEXT NOT NULL,
    seen BOOLEAN DEFAULT FALSE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL,
    date_created TIMESTAMP DEFAULT now() NOT NULL,
    message TEXT NOT NULL,
    PRIMARY KEY(id)
);
