(ns facepalm.c210-2015090301
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150903.01")

(def list-jobs-with-duplicates-query
  (str "select * from "
       "(select *, count(condor_id) over (partition by condor_id) as ct from jobs) "
       "sub where ct > 1 and submitter != '';"))

(defn- list-jobs-with-duplicates
  "Lists the jobs in the database for which there are duplicates without listing the
   duplicates themselves. The submitter ID isn't empty for any original job record."
  []
  (exec-raw list-jobs-with-duplicates-query :results))

(defn- first-where
  "Returns the first column value in the result set for which the predicate returns true."
  [k p s]
  (k (first (filter (comp p k) s))))

(defn- first-where-not
  "Returns the first column value in the result set for which the predicate returns false."
  [k p s]
  (first-where k (complement p) s))

(defn- build-full-job-entry
  "Builds a fully populated job entry for the job with the given ID."
  [orig dups]
  (assoc orig
    :batch_id          (first-where-not :batch_id nil? dups)
    :invocation_id     (first-where-not :invocation_id nil? dups)
    :app_id            (first-where-not :app_id nil? dups)
    :exit_code         (or (first-where-not :exit_code zero? dups) 0)
    :failure_threshold (or (first-where-not :failure_threshold zero? dups) 0)
    :failure_count     (apply + (map :failure_count dups))))

(def keys-to-update
  [:batch_id :invocation_id :app_id :exit_code :failure_threshold :failure_count])

(defn- update-job-entry
  "Updates a job entry with values obtained from its duplicates."
  [{:keys [:id] :as full-entry}]
  (update :jobs
          (set-fields (select-keys full-entry keys-to-update))
          (where {:id id})))

(defn- fix-foreign-keys
  "Fixes foriegn keys that reference jobs to be removed."
  [table column keeper-id ids-to-remove]
  (update table
          (set-fields {column keeper-id})
          (where {column [in ids-to-remove]})))

(def most-recent-condor-job-event-query
  (str "select id from "
       "(select *, max(date_triggered) over (partition by job_id) as max_date "
       " from condor_job_events "
       " where job_id = ?) as foo "
       "where date_triggered = max_date "
       "limit 1"))

(defn- most-recent-condor-job-event
  "Finds the ID of the most recent condor job event for a job."
  [job-id]
  (:id (first (exec-raw [most-recent-condor-job-event-query [job-id]] :results))))

(defn- fix-last-condor-job-event
  "Replaces the last condor job events for all duplicate jobs with the event that was
   triggered most recently."
  [keeper-id ids]
  (delete :last_condor_job_events (where {:job_id [in ids]}))
  (insert :last_condor_job_events
          (values {:job_id              keeper-id
                   :condor_job_event_id (most-recent-condor-job-event keeper-id)})))

(defn- delete-duplicate-jobs
  "Fixes foreign keys that refer to jobs that will be deleted."
  [keeper-id ids]
  (let [ids-to-remove (disj ids keeper-id)]
    (fix-foreign-keys :condor_job_deps :predecessor_id keeper-id ids-to-remove)
    (fix-foreign-keys :condor_job_deps :successor_id keeper-id ids-to-remove)
    (fix-foreign-keys :condor_job_events :job_id keeper-id ids-to-remove)
    (fix-foreign-keys :condor_job_stop_requests :job_id keeper-id ids-to-remove)
    (fix-foreign-keys :condor_raw_events :job_id keeper-id ids-to-remove)
    (fix-last-condor-job-event keeper-id ids)
    (delete :jobs (where {:id [in ids-to-remove]}))))

(defn- fix-duplicate-job-entries
  "Removes duplicate entries in the jobs table."
  []
  (println "\t* removing duplicate job entries")
  (doseq [job (list-jobs-with-duplicates)]
    (println (str "\t\t* removing duplicates for " (:id job)))
    (let [dups (select :jobs (where {:condor_id (:condor_id job)}))]
      (update-job-entry (build-full-job-entry job dups))
      (delete-duplicate-jobs (:id job) (set (map :id dups))))))

(defn- add-condor-id-uniqueness-constraint
  "Adds a uniqueness constraint on the condor_id column of the jobs table."
  []
  (println "\t* adding a uniqueness constraint on the condor_id column of the jobs table")
  (exec-raw "alter table only jobs add constraint condor_id_key unique (condor_id)"))

(defn convert
  "Performs the conversion."
  []
  (println "Performing the conversion for" version)
  (fix-duplicate-job-entries)
  (add-condor-id-uniqueness-constraint))
