(ns facepalm.c193-2014110503
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.3:20141105.03")

(defn- change-type
  []
  (println "\t* dropping not null constraint from the app_id column of the jobs table")
  (exec-raw "ALTER TABLE ONLY condor_events ALTER COLUMN event_number SET DATA TYPE character varying(3)")
  (exec-raw "ALTER TABLE ONLY condor_events ALTER COLUMN event_number SET NOT NULL"))

(defn convert
  "Performs the conversion for database version 1.9.3:20141105.03"
  []
  (println "Performing the conversion for" version)
  (change-type))
