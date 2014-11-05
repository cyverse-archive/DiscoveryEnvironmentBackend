(ns facepalm.c193-2014110502
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141105.02")

(defn- drop-constraint
  []
  (println "\t* dropping not null constraint from the app_id column of the jobs table")
  (exec-raw "ALTER TABLE ONLY jobs DROP CONSTRAINT batch_id_fkey"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141105.02"
  []
  (println "Performing the conversion for" version)
  (drop-constraint))
