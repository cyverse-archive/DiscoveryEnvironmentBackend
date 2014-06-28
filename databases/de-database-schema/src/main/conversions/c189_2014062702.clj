(ns facepalm.c189-2014062702
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140627.02")

(defn- add-job-steps-table
  []
  (println "\t* adding the job_steps table")
  (exec-raw
   "CREATE TABLE job_steps (
    id uuid NOT NULL,
    job_id uuid NOT NULL REFERENCES jobs(id),
    external_id character varying(40) NOT NULL,
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    job_type_id bigint NOT NULL REFERENCES job_types(id),
    app_step_number integer NOT NULL,
    PRIMARY KEY (id))"))

(defn- populate-job-steps-table
  []
  (println "\t* populating the job_steps table")
  (->> (select :jobs (fields :id :external_id :start_date :end_date :status :job_type_id))
       (map (fn [m] (assoc m
                      :job_id          (:id m)
                      :id              (UUID/randomUUID)
                      :app_step_number 1)))
       (map (fn [m] (insert :job_steps (values m))))
       (dorun)))

(defn- update-jobs-table
  []
  (println "\t* updating the jobs table")
  (exec-raw "ALTER TABLE jobs DROP COLUMN external_id")
  (exec-raw "ALTER TABLE jobs ADD COLUMN app_id character varying(255)")
  (exec-raw "ALTER TABLE jobs DROP COLUMN job_type_id"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-job-steps-table)
  (populate-job-steps-table)
  (update-jobs-table))
