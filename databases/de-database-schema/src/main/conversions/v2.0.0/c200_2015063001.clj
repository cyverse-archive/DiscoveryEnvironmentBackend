(ns facepalm.c200-2015063001
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150630.01")

(defn- increase-external-job-id-column-width
  []
  (println "\t* Increasing the width of the `external_id` field in the `job_steps` table")
  (exec-raw "ALTER TABLE job_steps ALTER COLUMN external_id TYPE character varying(64)"))

(defn convert
  []
  (println "Performing the conversion for" version)
  (increase-external-job-id-column-width))
