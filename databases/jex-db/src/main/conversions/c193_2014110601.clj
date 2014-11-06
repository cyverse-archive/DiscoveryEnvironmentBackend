(ns facepalm.c193-2014110601
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141106.01")

(defn- add-column
  []
  (println "\t* adds the invocation_id column to the jobs table")
  (exec-raw "ALTER TABLE ONLY jobs ADD COLUMN invocation_id uuid"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141106.01"
  []
  (println "Performing the conversion for" version)
  (add-column))
