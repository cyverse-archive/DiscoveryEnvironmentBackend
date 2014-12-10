(ns job-preserver.core
  (:gen-class)
  (:use [kameleon.core]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [korma.db])
  (:require [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [kameleon.jobs :as kj]
            [kameleon.pgpass :as kp]
            [monger.collection :as mc]
            [monger.core :as mg]))

(def ^:private timestamp-parser
  (tf/formatter (t/default-time-zone)
                "EEE MMM dd YYYY HH:mm:ss 'GMT'Z"
                "YYYY MMM dd HH:mm:ss"
                "YYYY-MM-dd-HH-mm-ss.SSS"
                "YYYY-MM-dd HH:mm:ss.SSS"
                "YYYY-MM-dd'T'HH:mm:ss.SSSZ"))

(defn- strip-time-zone
  "Removes the time zone abbreviation from a date timestamp."
  [s]
  (string/replace s #"\s*\(\w+\)\s*$" ""))

(defn- parse-timestamp
  "Parses a timestamp in one of the accepted formats, returning the number of milliseconds
  since the epoch."
  [s]
  (.getMillis (tf/parse timestamp-parser (strip-time-zone s))))

(defn- millis-from-str
  "Parses a string representation of a timestamp."
  [s]
  (assert (or (nil? s) (string? s)))
  (cond (or (string/blank? s) (= "0" s)) nil
        (re-matches #"\d+" s)            (Long/parseLong s)
        :else                            (parse-timestamp s)))

(defn- timestamp-from-str
  "Converts a string representation of a timestamp to a timestamp."
  [s]
  (when-let [millis (millis-from-str s)]
    (java.sql.Timestamp. millis)))

(defn- get-timestamp
  "Gets an instance of java.sql.Timestamp for the given timestamp."
  [t]
  (cond (number? t) (java.sql.Timestamp. t)
        (string? t) (timestamp-from-str t)
        :else       nil))

(def ^:private cli-options
  [[nil "--pg-host HOST" "PostgreSQL host name or IP address."
    :default "localhost"]
   [nil "--pg-port PORT" "PostgreSQL port number."
    :default 5432
    :parse-fn #(Integer/parseInt %)]
   [nil "--pg-db DATABASE" "PostgreSQL database name."
    :default "de"]
   [nil "--pg-user USER" "PostgreSQL username."
    :default "de"]
   [nil "--mongo-host HOST" "MongoDB host name or IP address."
    :default "localhost"]
   [nil "--mongo-port PORT" "MongoDB port number."
    :default 31395
    :parse-fn #(Integer/parseInt %)]
   [nil "--mongo-db DATABASE" "MongoDB database name."
    :default "osmdb"]
   [nil "--jobs-collection COLLECTION" "MongoDB collection name for jobs."
    :default "jobs"]
   [nil "--job-requests-collection COLLECTION" "MongoDB collection name for job requests."
    :default "job_requests"]
   [nil "--help"]])

(defn- usage
  [options-summary]
  (->> ["Moves job information from MongoDB to Postgres. Most of the job information"
        "has already been moved, but deleted jobs and jobs associated with apps that"
        "no longer exist were not. This tool moves the remaining jobs over."
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn- error-msg
  [errors]
  (str "Incorrect command usage:\n\n"
       (string/join \newline errors)))

(defn- exit
  [status msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit status))

(defn- parse-args
  [args]
  (let [{:keys [options _ errors summary]} (cli/parse-opts args cli-options)]
    (cond
     (:help options) (exit 0 (usage summary))
     errors          (exit 1 (error-msg errors)))
    options))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the database
  using Korma."
  [{:keys [pg-host pg-port pg-db pg-user]} pg-pass]
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     (str "//" pg-host ":" pg-port "/" pg-db)
   :user        pg-user
   :password    pg-pass})

(defn- prompt-for-password
  [pg-user]
  (print pg-user "password: ")
  (flush)
  (.. System console readPassword))

(defn- get-pg-pass
  [{:keys [pg-host pg-port pg-db pg-user]}]
  (or (kp/get-password pg-host pg-port pg-db pg-user)
      (prompt-for-password pg-user)))

(defn- define-database
  "Defines the database connection to use from within Clojure."
  [opts]
  (let [spec (create-db-spec opts (get-pg-pass opts))]
    (defonce de (create-db spec))
    (default-connection de)))

(defn- build-mongo-options
  []
  (mg/mongo-options
   :connections-per-host 10
   :max-wait-time        1200
   :connect-timeout      1000
   :socket-timeout       0
   :auto-connect-retry   true))

(defn- get-mongo-conn
  [{:keys [mongo-host mongo-port]}]
  (mg/connect! (mg/server-address mongo-host mongo-port) (build-mongo-options)))

(defn- get-mongo-db
  [{:keys [mongo-db]} mongo-conn]
  (mg/get-db mongo-conn mongo-db))

(defn- migrate-job
  [job]
  (println (:uuid job)))

(defn- exists-in-postgres?
  [{:keys [uuid]}]
  ((comp pos? :count first)
   (select :jobs
           (aggregate (count :*) :count)
           (where {:id (uuidify (string/replace uuid #"^j" ""))}))))

(defn- user-id-subselect
  [username]
  (subselect :users
             (fields :id)
             (where {:username (str username "@iplantcollaborative.org")})))

(defn- insert-submission
  [job-id submission]
  (when submission
    (exec-raw ["UPDATE jobs SET submission = CAST ( ? AS json ) WHERE id = ?"
              [(cast Object (cheshire/encode submission)) job-id]])))

(defn- get-job-submission
  [job-requests-collection orig-id]
  ((comp :experiment :state first)
   (mc/find-maps job-requests-collection {:state.jobUuid orig-id})))

(defn- insert-job
  [job-requests-collection state]
  (let [orig-id (:uuid state)
        job-id  (uuidify (string/replace orig-id #"^j" ""))]
    (insert :jobs
            (values {:id                 job-id
                     :job_name           (:name state)
                     :job_description    (:description state)
                     :app_id             (:analysis_id state)
                     :app_name           (:analysis_name state)
                     :app_description    (:analysis_description state)
                     :result_folder_path (:output_dir state)
                     :start_date         (get-timestamp (:submission_date state))
                     :end_date           (get-timestamp (:completion_date state))
                     :status             (:status state)
                     :deleted            (:deleted state false)
                     :user_id            (user-id-subselect (:user state))}))
    (insert-submission job-id (get-job-submission job-requests-collection orig-id))))

(defn- insert-job-step
  [state]
  (insert :job_steps
          (values {:job_id          (uuidify (string/replace (:uuid state) #"^j" ""))
                   :step_number     1
                   :external_id     (:uuid state)
                   :start_date      (get-timestamp (:submission_date state))
                   :end_date        (get-timestamp (:completion_date state))
                   :status          (:status state)
                   :job_type_id     (subselect :job_types (fields :id) (where {:name "DE"}))
                   :app_step_number 1})))

(defn- save-job
  [job-requests-collection state]
  (insert-job job-requests-collection state)
  (insert-job-step state))

(defn- run-conversion
  [{:keys [jobs-collection job-requests-collection]}]
  (->> (mc/find-maps jobs-collection)
       (map :state)
       (remove (comp nil? :uuid))
       (remove exists-in-postgres?)
       (map (partial save-job job-requests-collection))
       (dorun)))

(defn -main
  [& args]
  (let [opts    (parse-args args)
        mg-conn (get-mongo-conn opts)]
    (mg/set-db! (get-mongo-db opts mg-conn))
    (define-database opts)
    (run-conversion opts)))
