(ns facepalm.c193-2014103101
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141031.01")

(defn- add-condor-id-column
  []
  (println "\t* updating condor_id column in the jobs table")
  (exec-raw "ALTER TABLE jobs ADD COLUMN condor_id varchar(32) not null"))

(defn convert
  "Performs the conversion for database version 1.9.4:20141031.01"
  []
  (println "Performing the conversion for" version)
  (add-condor-id-column))
