(ns facepalm.c187-2014050901
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.7:20140509.01")

(defn- add-job-comment-column
  []
  (println "\t* adding the comment column to the jobs table")
  (exec-raw
   "ALTER TABLE jobs ADD COLUMN job_description text")
  (exec-raw
   "ALTER TABLE jobs ALTER COLUMN job_description SET DEFAULT ''"))

(defn convert
  "Performs the conversion for database version 1.8.6:20140509.01"
  []
  (println "Performing conversion for " version)
  (add-job-comment-column))
