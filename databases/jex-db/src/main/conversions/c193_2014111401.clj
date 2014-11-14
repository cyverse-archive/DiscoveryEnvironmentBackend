(ns facepalm.c193-2014111401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141114.01")

(defn- add-column
  []
  (println "\t* adds the checksum column to the condor_job_events table")
  (exec-raw "ALTER TABLE ONLY condor_job_events ADD COLUMN checksum varchar(64)"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141114.01"
  []
  (println "Performing the conversion for" version)
  (add-column))
