(ns kameleon.jobs
  (:use [kameleon.entities]
         [korma.core :exclude [update]]
         [korma.db :only [transaction]]
         [slingshot.slingshot :only [throw+]]))

(defn get-job-type-id
  "Fetches the primary key for the job type with the given name."
  [job-type]
  ((comp :id first) (select :job_types (where {:name job-type}))))

(defn- save-job-submission
  "Associated a job submission with a saved job in the database."
  [job-id submission]
  (exec-raw ["UPDATE jobs SET submission = CAST ( ? AS json ) WHERE id = ?"
             [(cast Object submission) job-id]]))

(defn- save-job*
  "Saves information about a job in the database."
  [job-info]
  (insert :jobs
    (values (select-keys job-info [:id
                                   :parent_id
                                   :job_name
                                   :job_description
                                   :app_id
                                   :app_name
                                   :app_description
                                   :app_wiki_url
                                   :result_folder_path
                                   :start_date
                                   :end_date
                                   :status
                                   :deleted
                                   :notify
                                   :user_id]))))

(defn save-job
  "Saves information about a job in the database."
  [job-info submission]
  (let [job-info (save-job* job-info)]
    (save-job-submission (:id job-info) submission)
    job-info))

(defn save-job-step
  "Saves a single job step in the database."
  [job-step]
  (insert :job_steps
    (values (select-keys job-step [:job_id
                                   :step_number
                                   :external_id
                                   :start_date
                                   :end_date
                                   :status
                                   :job_type_id
                                   :app_step_number]))))
