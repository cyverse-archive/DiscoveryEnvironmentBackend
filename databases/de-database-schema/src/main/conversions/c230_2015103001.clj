(ns facepalm.c230-2015103001
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.3.0:20151030.01")

(defn- add-job-status-table
  []
  (println "\t* Adding job status table")
  (load-sql-file "tables/76_job_status_updates.sql")
  (load-sql-file "constraints/76_job_status_updates.sql"))

(defn convert
  "Performs the conversion for database version 2.3.0:20151030.01"
  []
  (println "Performing the conversion for" version)
  (add-job-status-table))
