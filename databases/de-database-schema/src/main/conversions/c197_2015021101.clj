(ns facepalm.c197-2015021101
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.7:20150211.01")

(defn- add-container-settings-id-column
  []
  (println "\t* Adding a column to the tools table")
  (exec-raw "ALTER TABLE ONLY tools ADD COLUMN container_settings_id uuid;")
  (load-sql-file "constraints/03_tools.sql"))

(defn convert
  "Performs the conversion for database version 1.9.7:20150211.01"
  []
  (println "Performing the conversion for" version)
  (add-container-settings-id-column))
