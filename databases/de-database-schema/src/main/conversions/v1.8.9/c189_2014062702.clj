(ns facepalm.c189-2014062702
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140627.02")

(defn- add-job-steps-table
  []
  (println "\t* adding the job_steps table")
  (exec-raw
   "CREATE TABLE job_steps (
    job_id uuid NOT NULL REFERENCES jobs(id),
    step_number integer NOT NULL,
    external_id character varying(40) NOT NULL,
    start_date timestamp,
    end_date timestamp,
    status character varying(64) NOT NULL,
    job_type_id bigint NOT NULL REFERENCES job_types(id),
    app_step_number integer NOT NULL,
    PRIMARY KEY (job_id, step_number))"))

(defn- populate-job-steps-table
  []
  (println "\t* populating the job_steps table")
  (->> (select :jobs (fields :id :external_id :start_date :end_date :status :job_type_id))
       (map (fn [m] (-> (assoc m
                          :job_id          (:id m)
                          :step_number     1
                          :app_step_number 1)
                        (dissoc :id))))
       (map (fn [m] (insert :job_steps (values m))))
       (dorun)))

(defn- update-jobs-table
  []
  (println "\t* updating the jobs table")
  (exec-raw "ALTER TABLE jobs DROP COLUMN external_id")
  (exec-raw "ALTER TABLE jobs ADD COLUMN app_id character varying(255)")
  (exec-raw "ALTER TABLE jobs DROP COLUMN job_type_id")
  (exec-raw "ALTER TABLE jobs ADD COLUMN app_wiki_url text")
  (exec-raw "ALTER TABLE jobs ADD COLUMN app_description text")
  (exec-raw "ALTER TABLE jobs ADD COLUMN result_folder_path text"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-job-steps-table)
  (populate-job-steps-table)
  (update-jobs-table))
