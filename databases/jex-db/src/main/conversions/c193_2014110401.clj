(ns facepalm.c193-2014110401
  (:use [korma.core]))

(def :^private version
  "The destination database version."
  "1.9.3:20141104.01")

(defn- drop-columns
  []
  (println "\t* dropping command_line and env_variables columns from jobs table")
  (exec-raw "ALTER TABLE ONLY jobs DROP COLUMN IF EXISTS command_line")
  (exec-raw "ALTER TABLE ONLY jobs DROP COLUMN IF EXISTS env_variables"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141104.01"
  []
  (println "Performing the conversion for" version)
  (drop-columns))
