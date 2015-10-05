(ns facepalm.c180-2013080701
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130807.01")

(defn- add-user-agent-column
  "Adds a column for storing the user agent to the logins table."
  []
  (println "\t* adding the column for storing the User Agent to the logins table.")
  (exec-raw "ALTER TABLE logins ADD COLUMN user_agent text"))

(defn convert
  "Performs the conversion for database version 1.8.0:20130807.01."
  []
  (println "Performing conversion for" version)
  (add-user-agent-column))
