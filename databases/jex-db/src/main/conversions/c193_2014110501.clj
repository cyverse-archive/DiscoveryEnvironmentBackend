(ns facepalm.c193-2014110501
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141105.01")

(defn- drop-not-null
  []
  (println "\t* dropping not null constraint from the app_id column of the jobs table")
  (exec-raw "ALTER TABLE ONLY jobs ALTER COLUMN app_id DROP NOT NULL"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141104.01"
  []
  (println "Performing the conversion for" version)
  (drop-not-null))
