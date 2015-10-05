(ns facepalm.c184-2013101401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.4:20131014.01")

(defn- add-job-types-table
  "Adds the table that stores the types of jobs that the DE can submit."
  []
  (println "\t* creating the job_types table")
  (exec-raw "CREATE SEQUENCE job_types_id_seq")
  (exec-raw
   "CREATE TABLE job_types (
        id bigint DEFAULT nextval('job_types_id_seq'::regclass) NOT NULL,
        name character varying(36) NOT NULL,
        PRIMARY KEY (id))"))

(defn- add-jobs-table
  "Adds the table that stores information about jobs that the DE has."
  []
  (println "\t* creating the jobs table")
  (exec-raw
   "CREATE TABLE jobs (
        id uuid NOT NULL,
        external_id character varying(40) NOT NULL,
        job_name character varying(255) NOT NULL,
        app_name character varying(255),
        start_date timestamp,
        end_date timestamp,
        status character varying(64) NOT NULL,
        deleted boolean DEFAULT FALSE NOT NULL,
        job_type_id bigint REFERENCES job_types(id) NOT NULL,
        user_id bigint REFERENCES users(id) NOT NULL,
        PRIMARY KEY (id))"))

(defn- populate-job-types-table
  "Adds the types of jobs that the DE can submit."
  []
  (println "\t* populating the job_types table")
  (insert :job_types
          (values [{:name "DE"}
                   {:name "Agave"}])))

(defn convert
  "Performs the conversion for database version 1.8.4:20131014.01."
  []
  (println "Performing conversion for" version)
  (add-job-types-table)
  (add-jobs-table)
  (populate-job-types-table))
