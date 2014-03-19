(ns facepalm.c180-2013051601
  (:use [korma.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version"
  "1.8.0:20130516.01")

(defn- exec-statement
  "Executes a statement composed of one or more strings."
  [& strs]
  (exec-raw (string/join " " strs)))

(defn- create-acknowledgment-state-enum
  "Creates the acknowledgment state enumeration"
  []
  (println "\t* creating the acknowledgment_state enumeration UDT")
  (exec-statement 
    "CREATE TYPE acknowledgment_state"
    "  AS ENUM('unreceived', 'received', 'acknowledged', 'dismissed')"))

(defn- update-system-notification-acknowledgments-table
  "Updating the system notification acknowledgments table"
  []
  (println "\t* Updating the system notification acknowledgments")
  (exec-statement "DROP INDEX system_notification_acknowledgments_for_user_index")
  (exec-statement
    "ALTER TABLE system_notification_acknowledgments"
    "  ADD COLUMN state acknowledgment_state DEFAULT 'unreceived' NOT NULL")
  (exec-statement "UPDATE system_notification_acknowledgments SET state = 'acknowledged'")
  (exec-statement 
    "UPDATE system_notification_acknowledgments SET state = 'dismissed' WHERE deleted = TRUE")
  (exec-statement
    "ALTER TABLE system_notification_acknowledgments"
    "  DROP COLUMN id CASCADE,"
    "  DROP COLUMN deleted,"
    "  ALTER COLUMN date_acknowledged DROP NOT NULL,"
    "  ALTER COLUMN date_acknowledged SET DEFAULT NULL,"
    "  ADD PRIMARY KEY(user_id, system_notification_id)")
  (exec-statement "DROP SEQUENCE system_notification_acknowledgments_id_seq"))

(defn convert
  "Performs the conversion for database version 1.8.0:20130516.01."
  []
  (println "Performing the conversion for" version)
  (create-acknowledgment-state-enum)
  (update-system-notification-acknowledgments-table))
