(ns donkey.persistence.jobs
  "Functions for storing and retrieving information about jobs that the DE has
   submitted to any excecution service."
  (:use [clojure-commons.core :only [remove-nil-values]]
        [kameleon.queries :only [get-user-id]]
        [korma.core]
        [korma.db :only [with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
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
    "name" ['like (sqlfn :lower (str "%" value "%"))]
    "id" (sqlfn :lower value)
    value))

(defn- filter-field->where-field
  "Returns a field key for use in a job query where-clause map, based on the given filter field."
  [field]
  (case field
    "analysis_name" (sqlfn :lower :j.app_name)
    "name" (sqlfn :lower :j.job_name)
    "id" (sqlfn :lower :j.external_id)
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

(defn save-job
  "Saves information about a job in the database."
  [job-id job-name job-type username status & {:keys [id app-name start-date end-date deleted]}]
  (with-db db/de
    (let [job-type-id (get-job-type-id job-type)
          user-id     (get-user-id username)
          id          (or id (UUID/randomUUID))]
      (insert :jobs
              (values (remove-nil-values
                       {:id          id
                        :external_id (str job-id)
                        :job_name    job-name
                        :app_name    app-name
                        :start_date  start-date
                        :end_date    end-date
                        :status      status
                        :deleted     deleted
                        :job_type_id job-type-id
                        :user_id     user-id})))))
  {:id job-id
   :name job-name
   :status status
   :start-date (db/millis-from-timestamp start-date)})

(defn- count-jobs-base
  "The base query for counting the number of jobs in the database for a user."
  [username]
  (-> (select* [:jobs :j])
      (join [:users :u] {:j.user_id :u.id})
      (aggregate (count :*) :count)
      (where {:u.username username})))

(defn count-all-jobs
  "Counts the total number of jobs in the database for a user."
  [username]
  (with-db db/de
    ((comp :count first) (select (count-jobs-base username)))))

(defn count-jobs
  "Counts the number of undeleted jobs in the database for a user."
  [username job-types filter]
  (with-db db/de
    ((comp :count first)
     (select (add-job-query-filter-clause (count-jobs-base username) filter)
             (join [:job_types :jt] {:j.job_type_id :jt.id})
             (where {:jt.name   [in job-types]
                     :j.deleted false})))))

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
  (-> (select* [:jobs :j])
      (join [:users :u] {:j.user_id :u.id})
      (join [:job_types :jt] {:j.job_type_id :jt.id})
      (fields [:j.external_id :id]
              [:j.job_name    :name]
              [:j.app_name    :analysis_name]
              [:j.start_date  :startdate]
              [:j.end_date    :enddate]
              [:j.status      :status]
              [:jt.name       :job_type]
              [:u.username    :username])))

(defn- get-jobs
  "Gets a list of jobs satisfying a query."
  [username row-limit row-offset sort-field sort-order filter job-types]
  (with-db db/de
    (select (add-job-query-filter-clause (job-base-query) filter)
            (where {:j.deleted  false
                    :u.username username
                    :jt.name    [in job-types]})
            (order sort-field sort-order)
            (offset (nil-if-zero row-offset))
            (limit (nil-if-zero row-limit)))))

(defn list-jobs-of-types
  [username limit offset sort-field sort-order filter job-types]
  (get-jobs username limit offset sort-field sort-order filter job-types))

(defn- add-job-type-clause
  "Adds a where clause for a set of job types if the set of job types provided is not nil
   or empty."
  [query job-types]
  (assert (or (nil? job-types) (sequential? job-types)))
  (if-not (empty? job-types)
    (where query {:jt.name [in job-types]})
    query))

(defn get-external-job-ids
  "Gets a list of external job identifiers satisfying a query."
  [username {:keys [job-types]}]
  (with-db db/de
    (->> (-> (select* [:jobs :j])
             (join [:users :u] {:j.user_id :u.id})
             (join [:job_types :jt] {:j.job_type_id :jt.id})
             (fields [:j.external_id :id])
             (where {:u.username username})
             (add-job-type-clause job-types)
             (select))
         (map :id))))

(defn get-job-by-id
  "Gets a single job by its internal identifier."
  [id]
  (with-db db/de
    (first (select (job-base-query) (where {:j.id id})))))

(defn get-job-by-external-id
  "Gets a single job by its external identifier."
  [id]
  (with-db db/de
    (first (select (job-base-query) (where {:j.external_id id})))))

(defn update-job
  "Updates an existing job in the database."
  ([id {:keys [status end-date deleted]}]
     (with-db db/de
       (update :jobs
               (set-fields (remove-nil-values {:status   status
                                               :end_date end-date
                                               :deleted  deleted}))
               (where {:external_id id}))))
  ([id status end-date]
     (update-job id {:status   status
                     :end-date end-date})))

(defn update-job-by-internal-id
  "Updates an existing job in the database using the internal identifier as the key."
  [id {:keys [status end-date deleted]}]
  (with-db db/de
    (update :jobs
            (set-fields (remove-nil-values {:status   status
                                            :end_date end-date
                                            :deleted  deleted}))
            (where {:id id}))))

(defn list-incomplete-jobs
  []
  (with-db db/de
    (select [:jobs :j]
            (join [:users :u] {:j.user_id :u.id})
            (join [:job_types :jt] {:j.job_type_id :jt.id})
            (fields [:j.id          :id]
                    [:j.external_id :external_id]
                    [:j.status      :status]
                    [:u.username    :username]
                    [:jt.name       :job_type])
            (where {:j.deleted  false
                    :j.end_date nil}))))

(defn list-jobs-to-delete
  [ids]
  (with-db db/de
    (select [:jobs :j]
            (fields [:j.external_id :id]
                    [:j.deleted     :deleted])
            (where {:j.external_id [in ids]}))))

(defn delete-jobs
  [ids]
  (with-db db/de
    (update :jobs
           (set-fields {:deleted true})
           (where {:external_id [in ids]}))))
