
--
-- Stores acknowledgments of system notifications.
--
-- CAUTION: When the state field of a record is changed to 'acknowledged', the date_acknowledged
-- field should be set to the current time. We should consider using a trigger to enforce this 
-- constraint.
--
CREATE TABLE system_notification_acknowledgments (
    user_id BIGINT REFERENCES users(id) NOT NULL,
    system_notification_id BIGINT REFERENCES system_notifications(id) NOT NULL,
    state acknowledgment_state DEFAULT 'unreceived' NOT NULL,
    date_acknowledged TIMESTAMP DEFAULT NULL,
    
    PRIMARY KEY(user_id, system_notification_id)
);
