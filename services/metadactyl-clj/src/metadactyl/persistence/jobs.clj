(ns metadactyl.persistence.jobs
  "Functions for storing and retrieving information about jobs that the DE has
   submitted to any excecution service."
  (:use [clojure-commons.core :only [remove-nil-values]]
        [kameleon.queries :only [get-user-id]]
        [kameleon.uuids :only [uuidify]]
        [korma.core :exclude [update]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.set :as set]
            [clojure.string :as string]
            [kameleon.jobs :as kj]
            [korma.core :as sql]))

(def de-job-type "DE")
(def agave-job-type "Agave")

(def de-client-name "de")
(def agave-client-name "agave")
(def combined-client-name "combined")

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

(defn valid-status?
  "Determines whether or not the given status is a valid status code in the DE."
  [status]
  (contains? job-status-order status))

(defn status-follows?
  "Determines whether or not the new job status follows the old job status."
  [new-status old-status]
  (> (job-status-order new-status) (job-status-order old-status)))

(defn completed?
  [job-status]
  (completed-status-codes job-status))

(defn running?
  [job-status]
  (= running-status job-status))

(def not-completed? (complement completed?))

(defn- nil-if-zero
  "Returns nil if the argument value is zero."
  [v]
  (if (zero? v) nil v))

(defn- filter-value->where-value
  "Returns a value for use in a job query where-clause map, based on the given filter field and
   value pair."
  [field value]
  (case field
    "app_name"  ['like (sqlfn :lower (str "%" value "%"))]
    "name"      ['like (sqlfn :lower (str "%" value "%"))]
    "id"        (when-not (string/blank? value) (uuidify value))
    "parent_id" (when-not (string/blank? value) (uuidify value))
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
  (if (empty? filter)
    query
    (where query (apply or (map filter-map->where-clause filter)))))

(defn save-job
  "Saves information about a job in the database."
  [{:keys [id job-name description app-id app-name app-description app-wiki-url result-folder-path
           start-date end-date status deleted username notify parent-id]}
   submission]
  (let [user-id (get-user-id username)
        job-info (remove-nil-values
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
                    :user_id            user-id
                    :notify             notify
                    :parent_id          parent-id})]
    (kj/save-job job-info (cheshire/encode submission))))

(defn save-job-step
  "Saves a single job step in the database."
  [{:keys [job-id step-number external-id start-date end-date status job-type app-step-number]}]
  (let [job-type-id (kj/get-job-type-id job-type)]
    (when (nil? job-type-id)
      (throw+ {:type     :clojure-commons.exception/missing-request-field
               :error    "Job type id missing"
               :job-type job-type}))
    (kj/save-job-step
      (remove-nil-values
        {:job_id          job-id
         :step_number     step-number
         :external_id     external-id
         :start_date      start-date
         :end_date        end-date
         :status          status
         :job_type_id     job-type-id
         :app_step_number app-step-number}))))

(defn save-multistep-job
  [job-info job-steps submission]
  (save-job job-info submission)
  (dorun (map save-job-step job-steps)))

(defn- job-type-subselect
  [types]
  (subselect [:job_steps :s]
             (join [:job_types :t] {:s.job_type_id :t.id})
             (where {:j.id   :s.job_id
                     :t.name [not-in types]})))

(defn- internal-app-subselect
  []
  (subselect :apps
             (join :app_steps {:apps.id :app_steps.app_id})
             (join :tasks {:app_steps.task_id :tasks.id})
             (join :tools {:tasks.tool_id :tools.id})
             (join :tool_types {:tools.tool_type_id :tool_types.id})
             (where (and (= :j.app_id (raw "CAST(apps.id AS text)"))
                         (= :tool_types.name "internal")))))

(defn- add-internal-app-clause
  [query include-hidden]
  (if-not include-hidden
    (where query (not (exists (internal-app-subselect))))
    query))

(defn- count-jobs-base
  "The base query for counting the number of jobs in the database for a user."
  [username include-hidden]
  (-> (select* [:jobs :j])
      (join [:users :u] {:j.user_id :u.id})
      (aggregate (count :*) :count)
      (where {:u.username username})
      (add-internal-app-clause include-hidden)))

(defn count-jobs
  "Counts the number of undeleted jobs in the database for a user."
  [username filter include-hidden]
  ((comp :count first)
   (select (add-job-query-filter-clause (count-jobs-base username include-hidden) filter)
           (where {:j.deleted false}))))

(defn count-jobs-of-types
  "Counts the number of undeleted jobs of the given types in the database for a user."
  [username filter include-hidden types]
  ((comp :count first)
   (select (add-job-query-filter-clause (count-jobs-base username include-hidden) filter)
           (where {:j.deleted false})
           (where (not (exists (job-type-subselect types)))))))

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
              [:j.job_type           :job-type]
              [:j.parent_id          :parent-id]
              [:j.is_batch           :is-batch]
              [:j.notify             :notify])))

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
  [username row-limit row-offset sort-field sort-order filter include-hidden]
  (-> (select* (add-job-query-filter-clause (job-base-query) filter))
      (where {:j.deleted  false
              :j.username username})
      (add-internal-app-clause include-hidden)
      (order (translate-sort-field sort-field) sort-order)
      (offset (nil-if-zero row-offset))
      (limit (nil-if-zero row-limit))
      (select)))

(defn list-jobs-of-types
  "Gets a list of jobs that contain only steps of the given types."
  [username row-limit row-offset sort-field sort-order filter include-hidden types]
  (-> (select* (add-job-query-filter-clause (job-base-query) filter))
      (where {:j.deleted  false
              :j.username username})
      (where (not (exists (job-type-subselect types))))
      (add-internal-app-clause include-hidden)
      (order (translate-sort-field sort-field) sort-order)
      (offset (nil-if-zero row-offset))
      (limit (nil-if-zero row-limit))
      (select)))

(defn list-child-jobs
  "Lists the child jobs within a batch job."
  [batch-id]
  (select (job-base-query)
          (where {:parent_id batch-id})))

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
                 (where {:j.id (uuidify id)}))))

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
              [:j.notify             :notify]
              [:j.app_wiki_url       :app-wiki-url]
              [:j.submission         :submission]
              [:j.parent_id          :parent-id])
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
       (sql/update :jobs
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
    (sql/update :job_steps
      (set-fields (remove-nil-values {:external_id external-id
                                      :status      status
                                      :end_date    end-date
                                      :start_date  start-date}))
      (where {:job_id      job-id
              :step_number step-number}))))

(defn cancel-job-step-numbers
  "Marks a job step as canceled in the database."
  [job-id step-numbers]
  (sql/update :job_steps
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
    (sql/update :job_steps
      (set-fields (remove-nil-values {:status   status
                                      :end_date end-date}))
      (where {:job_id      job-id
              :external_id external-id}))))

(defn update-job-steps
  "Updates all steps for a job in the database."
  [job-id status end-date]
  (when (or status end-date)
    (sql/update :job_steps
      (set-fields (remove-nil-values {:status   status
                                      :end_date end-date}))
      (where {:job_id job-id}))))

(defn list-incomplete-jobs
  []
  (select (job-base-query)
          (where {:j.is_batch false
                  :j.deleted  false
                  :j.status   [not-in completed-status-codes]})))

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
  (sql/update :jobs
    (set-fields {:deleted true})
    (where {:id [in ids]})))

(defn get-jobs
  [ids]
  (select (job-base-query)
          (where {:j.id [in ids]})))

(defn list-non-existent-job-ids
  [job-id-set]
  (->> (get-jobs job-id-set)
       (map :id)
       (set)
       (set/difference job-id-set)))

(defn list-unowned-jobs
  [username job-ids]
  (select (job-base-query)
          (where {:j.id       [in (map uuidify job-ids)]
                  :j.username [not= username]})))
