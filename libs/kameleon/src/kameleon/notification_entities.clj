(ns kameleon.notification-entities
  (:use [korma.core]))

(declare users notifications analysis_execution_statuses email_notification_messages
         system_notification_types system_notifications system_notification_acknowledgments)

;; Information about users who have received notifications.
(defentity users
  (has-many notifications {:fk :user_id})
  (has-many system_notification_acknowledgments {:fk :user_id}))

;; The notifications themselves.
(defentity notifications
  (belongs-to users {:fk :user_id})
  (has-many email_notification_messages {:fk :notification_id}))

;; The most recent status seen by the notification agent for every job that it's seen.
(defentity analysis_execution_statuses)

;; Records of email messages sent in response to notifications.
(defentity email_notification_messages
  (belongs-to notifications {:fk :notification_id}))

;; Types of system notifications.
(defentity system_notification_types
  (has-many system_notifications {:fk :system_notification_type_id}))

;; System notifications.
(defentity system_notifications
  (belongs-to system_notification_types {:fk :system_notification_type_id})
  (has-many system_notification_acknowledgments {:fk :system_notification_id}))

;; Acknowledgments of system notifications.
(defentity system_notification_acknowledgments
  (belongs-to users {:fk :user_id})
  (belongs-to system_notifications {:fk :system_notification_id}))
