(ns facepalm.c199-2015053002
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.9:20150530.02")

(defn- add-container-settings-id-column
  []
  (println "\t* Adding a column to the tools table")
  (exec-raw "ALTER TABLE ONLY tools ADD COLUMN container_images_id uuid;")
  (load-sql-file "constraints/03_tools.sql"))

(defn convert
  "Performs the conversion for database version 1.9.9:20150530.02"
  []
  (println "Performing the conversion for" version)
  (add-container-settings-id-column))
