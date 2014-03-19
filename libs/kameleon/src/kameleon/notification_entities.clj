(ns kameleon.notification-entities
  (:use [korma.core]))

(declare users notifications analysis_execution_statuses email_notification_messages
         system_notification_types system_notifications system_notification_acknowledgments)

;; Information about users who have received notifications.
(defentity users
  (entity-fields :username)
  (has-many notifications {:fk :user_id})
  (has-many system_notification_acknowledgments {:fk :user_id}))

;; The notifications themselves.
(defentity notifications
  (entity-fields :uuid :type :subject :seen :deleted :date_created :message)
  (belongs-to users {:fk :user_id})
  (has-many email_notification_messages {:fk :notification_id}))

;; The most recent status seen by the notification agent for every job that it's seen.
(defentity analysis_execution_statuses
  (entity-fields :uuid :status :date_modified))

;; Records of email messages sent in response to notifications.
(defentity email_notification_messages
  (entity-fields :template :address :date_sent :payload)
  (belongs-to notifications {:fk :notification_id}))

;; Types of system notifications.
(defentity system_notification_types
  (entity-fields :name)
  (has-many system_notifications {:fk :system_notification_type_id}))

;; System notifications.
(defentity system_notifications
  (entity-fields :uuid :date_created :activation_date :deactivation_date :dismissible
                 :logins_disabled :message)
  (belongs-to system_notification_types {:fk :system_notification_type_id})
  (has-many system_notification_acknowledgments {:fk :system_notification_id}))

;; Acknowledgments of system notifications.
(defentity system_notification_acknowledgments
  (entity-fields :state :date_acknowledged)
  (belongs-to users {:fk :user_id})
  (belongs-to system_notifications {:fk :system_notification_id}))
