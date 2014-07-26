(ns donkey.persistence.jobs
  "Functions for storing and retrieving information about jobs that the DE has
   submitted to any excecution service."
  (:use [clojure-commons.core :only [remove-nil-values]]
        [kameleon.queries :only [get-user-id]]
        [korma.core]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.util.db :as db])
  (:import [java.util UUID]))

(def de-job-type "DE")
(def agave-job-type "Agave")

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
    "analysis_name" ['like (sqlfn :lower (str "%" value "%"))]
    "name"          ['like (sqlfn :lower (str "%" value "%"))]
    "id"            (sqlfn :lower value)
    value))

(defn- filter-field->where-field
  "Returns a field key for use in a job query where-clause map, based on the given filter field."
  [field]
  (case field
    "analysis_name" (sqlfn :lower :j.app_name)
    "name"          (sqlfn :lower :j.job_name)
    "id"            (sqlfn :lower :j.id)
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
  (with-db db/de
    (save-job* job-info)
    (save-job-submission (:id job-info) submission)))

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
  (with-db db/de (save-job-step* job-step)))

(defn save-multistep-job
  [job-info job-steps submission]
  (with-db db/de
    (transaction
     (save-job* job-info)
     (save-job-submission (:id job-info) submission)
     (dorun (map save-job-step* job-steps)))))

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
  (with-db db/de
    ((comp :count first) (select (count-jobs-base username)))))

(defn count-jobs
  "Counts the number of undeleted jobs in the database for a user."
  [username filter]
  (with-db db/de
    ((comp :count first)
     (select (add-job-query-filter-clause (count-jobs-base username) filter)
             (where {:j.deleted false})))))

(defn count-de-jobs
  "Counts the number of undeleted DE jobs int he database for a user."
  [username filter]
  (with-db db/de
    ((comp :count first)
     (select (add-job-query-filter-clause (count-jobs-base username) filter)
             (where {:j.deleted false})
             (where (not (sqlfn exists (agave-job-subselect))))))))

(defn count-null-descriptions
  "Counts the number of undeleted jobs with null descriptions in the database."
  [username]
  (with-db db/de
    ((comp :count first)
     (select (count-jobs-base username)
             (where {:j.deleted         false
                     :j.job_description nil})))))

(defn- translate-sort-field
  "Translates the sort field sent to get-jobs to a value that can be used in the query."
  [field]
  (case field
    :name          :j.job_name
    :analysis_name :j.app_name
    :startdate     :j.start_date
    :enddate       :j.end_date
    :status        :j.status))

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
      (join [:job_types :t] {:s.job_type_id :t.id})
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
   (with-db db/de
     (select (job-step-base-query)
             (where {:s.job_id      job-id
                     :s.external_id external-id})))))

(defn get-job-steps-by-external-id
  "Retrieves all of the job steps with an external identifier."
  [external-id]
  (with-db db/de
    (select (job-step-base-query)
            (where {:s.external_id external-id}))))

(defn get-max-step-number
  "Gets the maximum step number for a job."
  [job-id]
  (with-db db/de
    ((comp :max-step first)
     (select :job_steps
             (aggregate (max :step_number) :max-step)
             (where {:job_id job-id})))))

(defn list-jobs
  "Gets a list of jobs satisfying a query."
  [username row-limit row-offset sort-field sort-order filter]
  (with-db db/de
    (select (add-job-query-filter-clause (job-base-query) filter)
            (where {:j.deleted  false
                    :j.username username})
            (order (translate-sort-field sort-field) sort-order)
            (offset (nil-if-zero row-offset))
            (limit (nil-if-zero row-limit)))))

(defn list-de-jobs
  "Gets a list of jobs that contain only DE steps."
  [username row-limit row-offset sort-field sort-order filter]
  (with-db db/de
    (select (add-job-query-filter-clause (job-base-query) filter)
            (where {:j.deleted  false
                    :j.username username})
            (where (not (sqlfn exists (agave-job-subselect))))
            (order (translate-sort-field sort-field) sort-order)
            (offset (nil-if-zero row-offset))
            (limit (nil-if-zero row-limit)))))

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
  (with-db db/de
    (first (select (job-base-query)
                   (fields :submission)
                   (where {:j.id id})))))

(defn update-job
  "Updates an existing job in the database."
  ([id {:keys [status end-date deleted name description]}]
     (when (or status end-date deleted name description)
       (with-db db/de
         (update :jobs
                 (set-fields (remove-nil-values {:status          status
                                                 :end_date        end-date
                                                 :deleted         deleted
                                                 :job_name        name
                                                 :job_description description}))
                 (where {:id id})))))
  ([id status end-date]
     (update-job id {:status   status
                     :end-date end-date})))

(defn update-job-step-number
  "Updates an existing job step in the database using the job ID and the step number as keys."
  [job-id step-number {:keys [external-id status end-date start-date]}]
  (when (or external-id status end-date start-date)
    (with-db db/de
      (update :job_steps
              (set-fields (remove-nil-values {:external_id external-id
                                              :status      status
                                              :end_date    end-date
                                              :start_date  start-date}))
              (where {:job_id      job-id
                      :step_number step-number})))))

(defn get-job-step-number
  "Retrieves a job step from the database by its step number."
  [job-id step-number]
  (first
   (with-db db/de
     (select (job-step-base-query)
             (where {:s.job_id      job-id
                     :s.step_number step-number})))))

(defn update-job-step
  "Updates an existing job step in the database."
  [job-id external-id status end-date]
  (when (or status end-date)
    (with-db db/de
      (update :job_steps
              (set-fields (remove-nil-values {:status   status
                                              :end_date end-date}))
              (where {:job_id      job-id
                      :external_id external-id})))))

(defn list-incomplete-jobs
  []
  (with-db db/de
    (select (job-base-query)
            (where {:j.deleted  false
                    :j.end_date nil}))))

(defn list-job-steps
  [job-id]
  (with-db db/de
    (select (job-step-base-query)
            (where {:job_id job-id}))))

(defn list-jobs-to-delete
  [ids]
  (with-db db/de
    (select [:jobs :j]
            (fields [:j.id      :id]
                    [:j.deleted :deleted])
            (where {:j.id [in ids]}))))

(defn delete-jobs
  [ids]
  (with-db db/de
    (update :jobs
            (set-fields {:deleted true})
            (where {:id [in ids]}))))
