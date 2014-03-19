(ns facepalm.c180-2013020401
  (:use [korma.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130204.01")

(defn- exec-statement
  "Executes a statement composed of one or more strings."
  [& strs]
  (exec-raw (string/join " " strs)))

(defn- create-seq
  "Creates a sequence that starts with 1 and increments with 1 with no maximum minimum value."
  [seq-name]
  (exec-statement
   "CREATE SEQUENCE" seq-name
   "START WITH 1"
   "INCREMENT BY 1"
   "NO MAXVALUE"
   "NO MINVALUE"
   "CACHE 1"))

(defn- add-users-table
  "Creates the users table."
  []
  (println "\t* adding the users table")
  (create-seq "users_id_seq")
  (exec-statement
   "CREATE TABLE users ("
   "id BIGINT DEFAULT nextval('users_id_seq'::regclass) NOT NULL,"
   "username VARCHAR(512) UNIQUE NOT NULL,"
   "PRIMARY KEY(id))"))

(defn- populate-users-table
  "Populates the users table with information from the notifications table."
  []
  (println "\t* populating the users table")
  (exec-statement
   "INSERT INTO users (username)"
   "SELECT DISTINCT username FROM notifications"))

(defn- update-notifications-table
  "Replaces the username column with a user_id column in the notifications table."
  []
  (println "\t* replacing the uername column in the notifications table")
  (exec-statement
   "ALTER TABLE notifications"
   "ADD COLUMN user_id BIGINT REFERENCES users(id)")
  (exec-statement
   "UPDATE notifications"
   "SET user_id = (SELECT id FROM users u WHERE notifications.username = u.username)")
  (exec-statement
   "ALTER TABLE notifications"
   "DROP COLUMN username")
  (exec-statement
   "ALTER TABLE notifications"
   "ALTER COLUMN user_id SET NOT NULL"))

(defn- create-system-notification-types-table
  "Creates and populates the system notification types table."
  []
  (println "\t* creating and populating the system_notification_types table")
  (create-seq "system_notification_types_id_seq")
  (exec-statement
   "CREATE TABLE system_notification_types ("
   "id BIGINT DEFAULT nextval('system_notification_types_id_seq'::regclass) NOT NULL,"
   "name VARCHAR(32),"
   "PRIMARY KEY(id))")
  (insert :system_notification_types
          (values [{:name "announcement"}
                   {:name "maintenance"}
                   {:name "warning"}])))

(defn- create-system-notifications-table
  "Creates the system notifications table."
  []
  (println "\t* creating the system_notifications table")
  (create-seq "system_notifications_id_seq")
  (exec-statement
   "CREATE TABLE system_notifications ("
   "id BIGINT DEFAULT nextval('system_notifications_id_seq'::regclass) NOT NULL,"
   "uuid UUID NOT NULL,"
   "system_notification_type_id BIGINT REFERENCES system_notification_types(id) NOT NULL,"
   "date_created TIMESTAMP DEFAULT now() NOT NULL,"
   "activation_date TIMESTAMP DEFAULT now() NOT NULL,"
   "deactivation_date TIMESTAMP,"
   "dismissible BOOLEAN DEFAULT FALSE NOT NULL,"
   "logins_disabled BOOLEAN DEFAULT FALSE NOT NULL,"
   "message TEXT,"
   "PRIMARY KEY(id))"))

(defn- create-system-notification-acknowledgments-table
  "Creates the system_notification_acknowledgments table."
  []
  (println "\t* creating the system_notification_acknowledgments table")
  (create-seq "system_notification_acknowledgments_id_seq")
  (exec-statement
   "CREATE TABLE system_notification_acknowledgments ("
   "id BIGINT DEFAULT nextval('system_notification_acknowledgments_id_seq'::regclass) NOT NULL,"
   "user_id BIGINT REFERENCES users(id) NOT NULL,"
   "system_notification_id BIGINT REFERENCES system_notifications(id) NOT NULL,"
   "deleted BOOLEAN DEFAULT FALSE NOT NULL,"
   "date_acknowledged TIMESTAMP DEFAULT now() NOT NULL,"
   "PRIMARY KEY(id))")
  (exec-statement
   "CREATE UNIQUE INDEX system_notification_acknowledgments_for_user_index"
   "ON system_notification_acknowledgments(user_id, system_notification_id)"))

(defn convert
  "Performs the conversion for database version 1.8.0:20130204.01."
  []
  (println "Performing the conversion for" version)
  (add-users-table)
  (populate-users-table)
  (update-notifications-table)
  (create-system-notification-types-table)
  (create-system-notifications-table)
  (create-system-notification-acknowledgments-table))
