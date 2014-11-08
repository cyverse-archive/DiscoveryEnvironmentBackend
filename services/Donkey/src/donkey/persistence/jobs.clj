(ns donkey.persistence.jobs
  "Functions for storing and retrieving information about jobs that the DE has
   submitted to any excecution service."
  (:use [clojure-commons.core :only [remove-nil-values]]
        [kameleon.queries :only [get-user-id]]
        [korma.core]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.util.db :as db])
  (:import [java.util UUID]))

(def de-job-type "DE")
(def agave-job-type "Agave")

(def pending-status "Pending")
(def canceled-status "Canceled")
(def failed-status "Failed")
(def completed-status "Completed")
(def submitted-status "Submitted")
(def idle-status "Idle")
(def running-status "Running")
(def completed-status-codes #{canceled-status failed-status completed-status})

(def job-status-order
  {pending-status   0
   submitted-status 1
   idle-status      2
   running-status   3
   completed-status 4
   failed-status    4
   canceled-status  4})

(defn- nil-if-zero
  "Returns nil if the argument value is zero."
  [v]
  (if (zero? v) nil v))

(defn- get-job-type-id
  "Fetches the primary key for the job type with the given name."
  [job-type]
  (let [id ((comp :id first) (select :job_types (where {:name job-type})))]
    (when (nil? id)
      (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
               :job-type   job-type}))
    id))

(defn- filter-value->where-value
  "Returns a value for use in a job query where-clause map, based on the given filter field and
   value pair."
  [field value]
  (case field
    "app_name" ['like (sqlfn :lower (str "%" value "%"))]
    "name"     ['like (sqlfn :lower (str "%" value "%"))]
    "id"       (UUID/fromString value)
    value))

(defn- filter-field->where-field
  "Returns a field key for use in a job query where-clause map, based on the given filter field."
  [field]
  (case field
    "app_name" (sqlfn :lower :j.app_name)
    "name"     (sqlfn :lower :j.job_name)
    (keyword (str "j." field))))

(defn- filter-map->where-clause
  "Returns a map for use in a where-clause for filtering job query results."
  [{:keys [field value]}]
  {(filter-field->where-field field) (filter-value->where-value field value)})

(defn- add-job-query-filter-clause
  "Filters results returned by the given job query by adding a (where (or ...)) clause based on the
   given filter map."
  [query filter]
  (if (nil? filter)
    query
    (where query (apply or (map filter-map->where-clause filter)))))

(defn- save-job-submission
  "Associated a job submission with a saved job in the database."
  [job-id submission]
  (exec-raw ["UPDATE jobs SET submission = CAST ( ? AS json ) WHERE id = ?"
             [(cast Object (cheshire/encode submission)) job-id]]))

(defn- save-job*
  "Saves information about a job in the database."
  [{:keys [id job-name description app-id app-name app-description app-wiki-url result-folder-path
           start-date end-date status deleted username]}]
  (let [user-id (get-user-id username)]
    (insert :jobs
            (values (remove-nil-values
                     {:id                 id
                      :job_name           job-name
                      :job_description    description
                      :app_id             app-id
                      :app_name           app-name
                      :app_description    app-description
                      :app_wiki_url       app-wiki-url
                      :result_folder_path result-folder-path
                      :start_date         start-date
                      :end_date           end-date
                      :status             status
                      :deleted            deleted
                      :user_id            user-id})))))

(defn save-job
  "Saves information about a job in the database."
  [job-info submission]
  (save-job* job-info)
  (save-job-submission (:id job-info) submission))

(defn- save-job-step*
  "Saves a single job step in the database."
  [{:keys [job-id step-number external-id start-date end-date status job-type app-step-number]}]
  (let [job-type-id (get-job-type-id job-type)]
    (insert :job_steps
            (values (remove-nil-values
                     {:job_id job-id
                      :step_number     step-number
                      :external_id     external-id
                      :start_date      start-date
                      :end_date        end-date
                      :status          status
                      :job_type_id     job-type-id
                      :app_step_number app-step-number})))))

(defn save-job-step
  "Saves a single job step in the database."
  [job-step]
  (save-job-step* job-step))

(defn save-multistep-job
  [job-info job-steps submission]
  (save-job* job-info)
  (save-job-submission (:id job-info) submission)
  (dorun (map save-job-step* job-steps)))

(defn- agave-job-subselect
  []
  (subselect [:job_steps :s]
             (join [:job_types :t] {:s.job_type_id :t.id})
             (where {:j.id   :s.job_id
                     :t.name agave-job-type})))

(defn- count-jobs-base
  "The base query for counting the number of jobs in the database for a user."
  [username]
  (-> (select* [:jobs :j])
      (join [:users :u] {:j.user_id :u.id})
      (join [:job_steps :s] {:j.id :s.job_id})
      (join [:job_types :t] {:s.job_type_id :t.id})
      (aggregate (count :*) :count)
      (where {:u.username username})))

(defn count-all-jobs
  "Counts the total number of jobs in the database for a user."
  [username]
  ((comp :count first) (select (count-jobs-base username))))

(defn count-jobs
  "Counts the number of undeleted jobs in the database for a user."
  [username filter]
  ((comp :count first)
   (select (add-job-query-filter-clause (count-jobs-base username) filter)
           (where {:j.deleted false}))))

(defn count-de-jobs
  "Counts the number of undeleted DE jobs int he database for a user."
  [username filter]
  ((comp :count first)
   (select (add-job-query-filter-clause (count-jobs-base username) filter)
           (where {:j.deleted false})
           (where (not (exists (agave-job-subselect)))))))

(defn count-null-descriptions
  "Counts the number of undeleted jobs with null descriptions in the database."
  [username]
  ((comp :count first)
   (select (count-jobs-base username)
           (where {:j.deleted         false
                   :j.job_description nil}))))

(defn- translate-sort-field
  "Translates the sort field sent to get-jobs to a value that can be used in the query."
  [field]
  (case field
    :name      :j.job_name
    :app_name  :j.app_name
    :startdate :j.start_date
    :enddate   :j.end_date
    :status    :j.status))

(defn- job-base-query
  "The base query used for retrieving job information from the database."
  []
  (-> (select* [:job_listings :j])
      (fields [:j.app_description    :app-description]
              [:j.app_id             :app-id]
              [:j.app_name           :app-name]
              [:j.job_description    :description]
              [:j.end_date           :end-date]
              [:j.id                 :id]
              [:j.job_name           :job-name]
              [:j.result_folder_path :result-folder-path]
              [:j.start_date         :start-date]
              [:j.status             :status]
              [:j.username           :username]
              [:j.app_wiki_url       :app-wiki-url]
              [:j.job_type           :job-type])))

(defn- job-step-base-query
  "The base query used for retrieving job step information from the database."
  []
  (-> (select* [:job_steps :s])
      (join :inner [:job_types :t] {:s.job_type_id :t.id})
      (fields [:s.job_id          :job-id]
              [:s.step_number     :step-number]
              [:s.external_id     :external-id]
              [:s.start_date      :start-date]
              [:s.end_date        :end-date]
              [:s.status          :status]
              [:t.name            :job-type]
              [:s.app_step_number :app-step-number])))

(defn get-job-step
  "Retrieves a single job step from the database."
  [job-id external-id]
  (first
   (select (job-step-base-query)
           (where {:s.job_id      job-id
                   :s.external_id external-id}))))

(defn get-job-steps-by-external-id
  "Retrieves all of the job steps with an external identifier."
  [external-id]
  (select (job-step-base-query)
          (where {:s.external_id external-id})))

(defn get-max-step-number
  "Gets the maximum step number for a job."
  [job-id]
  ((comp :max-step first)
   (select :job_steps
           (aggregate (max :step_number) :max-step)
           (where {:job_id job-id}))))

(defn list-jobs
  "Gets a list of jobs satisfying a query."
  [username row-limit row-offset sort-field sort-order filter]
  (select (add-job-query-filter-clause (job-base-query) filter)
          (where {:j.deleted  false
                  :j.username username})
          (order (translate-sort-field sort-field) sort-order)
          (offset (nil-if-zero row-offset))
          (limit (nil-if-zero row-limit))))

(defn list-de-jobs
  "Gets a list of jobs that contain only DE steps."
  [username row-limit row-offset sort-field sort-order filter]
  (select (add-job-query-filter-clause (job-base-query) filter)
          (where {:j.deleted  false
                  :j.username username})
          (where (not (exists (agave-job-subselect))))
          (order (translate-sort-field sort-field) sort-order)
          (offset (nil-if-zero row-offset))
          (limit (nil-if-zero row-limit))))

(defn- add-job-type-clause
  "Adds a where clause for a set of job types if the set of job types provided is not nil
   or empty."
  [query job-types]
  (assert (or (nil? job-types) (sequential? job-types)))
  (if-not (empty? job-types)
    (where query {:jt.name [in job-types]})
    query))

(defn get-job-by-id
  "Gets a single job by its internal identifier."
  [id]
  (first (select (job-base-query)
                 (fields :submission)
                 (where {:j.id id}))))

(defn- lock-job*
  "Retrieves a job by its internal identifier, placing a lock on the row."
  [id]
  (-> (select* [:jobs :j])
      (fields [:j.app_description    :app-description]
              [:j.app_id             :app-id]
              [:j.app_name           :app-name]
              [:j.job_description    :description]
              [:j.end_date           :end-date]
              [:j.id                 :id]
              [:j.job_name           :job-name]
              [:j.result_folder_path :result-folder-path]
              [:j.start_date         :start-date]
              [:j.status             :status]
              [:j.app_wiki_url       :app-wiki-url]
              [:j.submission         :submission])
      (where {:j.id id})
      (#(str (as-sql %) " for update"))
      (#(exec-raw [% [id]] :results))
      (first)))

(defn- distinct-job-step-types
  "Obtains the list of distinct job step types associated with a job."
  [job-id]
  (map :job-type (select [:job_steps :s]
                         (join [:job_types :t] {:s.job_type_id :t.id})
                         (fields [:t.name :job-type])
                         (modifier "DISTINCT")
                         (where {:s.job_id job-id}))))

(defn- determine-job-type
  "Determines the type of a job in the database."
  [job-id]
  (let [job-step-types (distinct-job-step-types job-id)]
    (if (<= (count job-step-types) 1) (first job-step-types) de-job-type)))

(defn- determine-job-username
  "Determines the username of the user who submitted a job."
  [job-id]
  ((comp :username first)
   (select [:jobs :j]
           (join [:users :u] {:j.user_id :u.id})
           (fields [:u.username :username])
           (where {:j.id job-id}))))

(defn lock-job
  "Retrieves a job by its internal identifier, placing a lock on the row. For-update queries
   can't be used in conjunction with a group-by clause, so we have to use a separate query to
   determine the overall job type. A separate query also has to be used to retrieve the username
   so that a lock isn't placed on the users table.

   In most cases the MVCC behavior provided by Postgres is sufficient to ensure that the system
   stays in a consistent state. The one case where it isn't sufficient is when the status of
   a job is being updated. The reason the MVCC behavior doesn't work in this case is because a
   status update trigger a notification or another job in the case of a pipeline. Because of this,
   we need to ensure that only one thread is preparing to update a job status at any given time.

   Important note: in cases where both the job and the job step need to be locked, the job step
   should be locked first."
  [id]
  (when-let [job (lock-job* id)]
    (assoc job
      :job-type (determine-job-type id)
      :username (determine-job-username id))))

(defn- lock-job-step*
  "Retrieves a job step from the database by its job ID and external job ID, placing a lock on
   the row."
  [job-id external-id]
  (-> (select* [:job_steps :s])
      (fields [:s.job_id          :job-id]
              [:s.step_number     :step-number]
              [:s.external_id     :external-id]
              [:s.start_date      :start-date]
              [:s.end_date        :end-date]
              [:s.status          :status]
              [:s.app_step_number :app-step-number])
      (where (and {:s.job_id      job-id}
                  {:s.external_id external-id}))
      (#(str (as-sql %) " for update"))
      (#(exec-raw [% [job-id external-id]] :results))
      (first)))

(defn- determine-job-step-type
  "Dtermines the job type associated with a job step in the database."
  [job-id external-id]
  ((comp :job-type first)
   (select [:job_steps :s]
           (join [:job_types :t] {:s.job_type_id :t.id})
           (fields [:t.name :job-type])
           (where {:s.job_id      job-id
                   :s.external_id external-id}))))

(defn lock-job-step
  "Retrieves a job step by its associated job identifier and external job identifier. The lock on
   the job step is required in the same case and for the same reason as the lock on the job. Please
   see the documentation for lock-job for more details.

   Important note: in cases where both the job and the job step need to be locked, the job step
   should be locked first."
  [job-id external-id]
  (when-let [job-step (lock-job-step* job-id external-id)]
    (assoc job-step :job-type (determine-job-step-type job-id external-id))))

(defn update-job
  "Updates an existing job in the database."
  ([id {:keys [status end-date deleted name description]}]
     (when (or status end-date deleted name description)
       (update :jobs
               (set-fields (remove-nil-values {:status          status
                                               :end_date        end-date
                                               :deleted         deleted
                                               :job_name        name
                                               :job_description description}))
               (where {:id id}))))
  ([id status end-date]
     (update-job id {:status   status
                     :end-date end-date})))

(defn update-job-step-number
  "Updates an existing job step in the database using the job ID and the step number as keys."
  [job-id step-number {:keys [external-id status end-date start-date]}]
  (when (or external-id status end-date start-date)
    (update :job_steps
            (set-fields (remove-nil-values {:external_id external-id
                                            :status      status
                                            :end_date    end-date
                                            :start_date  start-date}))
            (where {:job_id      job-id
                    :step_number step-number}))))

(defn cancel-job-step-numbers
  "Marks a job step as canceled in the database."
  [job-id step-numbers]
  (update :job_steps
          (set-fields {:status     canceled-status
                       :start_date (sqlfn coalesce :start_date (sqlfn now))
                       :end_date   (sqlfn now)})
          (where {:job_id      job-id
                  :step_number [in step-numbers]})))

(defn get-job-step-number
  "Retrieves a job step from the database by its step number."
  [job-id step-number]
  (first
   (select (job-step-base-query)
           (where {:s.job_id      job-id
                   :s.step_number step-number}))))

(defn update-job-step
  "Updates an existing job step in the database."
  [job-id external-id status end-date]
  (when (or status end-date)
    (update :job_steps
            (set-fields (remove-nil-values {:status   status
                                            :end_date end-date}))
            (where {:job_id      job-id
                    :external_id external-id}))))

(defn list-incomplete-jobs
  []
  (select (job-base-query)
          (where {:j.deleted false
                  :j.status  [not-in completed-status-codes]})))

(defn list-job-steps
  [job-id]
  (select (job-step-base-query)
          (where {:job_id job-id})
          (order :step_number)))

(defn list-jobs-to-delete
  [ids]
  (select [:jobs :j]
          (join [:users :u] {:j.user_id :u.id})
          (fields [:j.id       :id]
                  [:j.deleted  :deleted]
                  [:u.username :user])
          (where {:j.id [in ids]})))

(defn delete-jobs
  [ids]
  (update :jobs
          (set-fields {:deleted true})
          (where {:id [in ids]})))
