/*
 * This file contains all of the user-defined types defined for this schema.
 */

--
-- The possible states of a user acknowledgment of a notification.
-- 
CREATE TYPE acknowledgment_state AS ENUM (
	'unreceived',   -- the user has not received the notification 
	'received',     -- the user has received but not seen the notification
	'acknowledged', -- the user has seen the notification
	'dismissed');   -- the user has dismissed the notification
