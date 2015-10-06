(ns facepalm.c189-2014071501
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20140715.01")

(defn- update-job-steps-table
  "Removes the not-null constraint on the external_id field of the job_steps table."
  []
  (println "\t* updating the job_steps table.")
  (exec-raw "ALTER TABLE job_steps ALTER COLUMN external_id DROP NOT NULL"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing conversion for" version)
  (update-job-steps-table))
