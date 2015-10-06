(ns facepalm.c189-2014071101
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140711.01")

(defn- add-job-submission-column
  "Adds the submission column to the jobs table in the database."
  []
  (println "\t* adding the submisison column to the jobs table")
  (exec-raw "ALTER TABLE jobs ADD COLUMN submission json"))

(defn convert
  "Performs the databse conversion."
  []
  (println "Performing the conversion for" version)
  (add-job-submission-column))
