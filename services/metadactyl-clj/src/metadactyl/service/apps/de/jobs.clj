(ns metadactyl.service.apps.de.jobs
  (:use [clojure-commons.file-utils :only [build-result-folder-path]]
        [kameleon.jobs :only [get-job-type-id save-job save-job-step]]
        [kameleon.queries :only [get-user-id]]
        [korma.core :only [sqlfn]]
        [korma.db :only [transaction]]
        [medley.core :only [dissoc-in]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [kameleon.db :as db]
            [metadactyl.clients.jex :as jex]
            [metadactyl.clients.jex-events :as jex-events]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.de.jobs.base :as jb]
            [metadactyl.util.json :as json-util]))

(defn- secured-params
  [user]
  {:user (:shortUsername user)})

(defn- pre-process-jex-step
  "Removes the input array of a fAPI step's config."
  [{{step-type :type} :component :as step}]
  (if (= step-type "fAPI")
    (dissoc-in step [:config :input])
    step))

(defn- pre-process-jex-submission
  "Finalizes the job for submission to the JEX."
  [job]
  (update-in job [:steps] (partial map pre-process-jex-step)))

(defn- do-jex-submission
  [job]
  (try+
   (jex/submit-job (pre-process-jex-submission job))
   (catch Object _
     (log/error (:throwable &throw-context) "job submission failed")
     (throw+ {:type  :clojure-commons.exception/request-failed
              :error "job submission failed"}))))

(defn- store-submitted-job
  "Saves information about a job in the database."
  [user job submission status]
  (let [job-info (-> job
                     (select-keys [:app_id :app_name :app_description :notify])
                     (assoc :job_name           (:name job)
                            :job_description    (:description job)
                            :app_wiki_url       (:wiki_url job)
                            :result_folder_path (:output_dir job)
                            :start_date         (sqlfn now)
                            :user_id            (get-user-id (:username user))
                            :status             status
                            :parent_id          (:parent_id submission)))]
    (save-job job-info (cheshire/encode submission))))

(defn- store-job-step
  "Saves a single job step in the database."
  [job-id job status]
  (save-job-step {:job_id          job-id
                  :step_number     1
                  :external_id     (:uuid job)
                  :start_date      (sqlfn now)
                  :status          status
                  :job_type_id     (get-job-type-id "DE")
                  :app_step_number 1}))

(defn- save-job-submission
  "Saves a DE job and its job-step in the database."
  ([user job submission]
     (save-job-submission user job submission "Submitted"))
  ([user job submission status]
     (transaction
      (let [job-id (:id (store-submitted-job user job submission status))]
        (store-job-step job-id job status)
        job-id))))

(defn- format-job-submission-response
  [user jex-submission batch?]
  (remove-nil-vals
   {:app_description (:app_description jex-submission)
    :app_disabled    false
    :app_id          (:app_id jex-submission)
    :app_name        (:app_name jex-submission)
    :batch           batch?
    :description     (:description jex-submission)
    :id              (str (:uuid jex-submission))
    :name            (:name jex-submission)
    :notify          (:notify jex-submission)
    :resultfolderid  (:output_dir jex-submission)
    :startdate       (str (.getTime (db/now)))
    :status          jp/submitted-status
    :username        (:username user)
    :wiki_url        (:wiki_url jex-submission)}))

(defn- submit-job
  [user submission job]
  (let [batch? (boolean (:parent_id submission))]
    (try+
     (do-jex-submission job)
     (save-job-submission user job submission)
     (catch Object _
       (if batch?
         (save-job-submission user job submission "Failed")
         (throw+))))
    (format-job-submission-response user job batch?)))

(defn- prep-submission
  [submission]
  (assoc submission
    :output_dir           (build-result-folder-path submission)
    :create_output_subdir false))

(defn- build-submission
  [user submission]
  (remove-nil-vals (jb/build-submission user submission)))

(defn submit
  [user submission]
  (->> (prep-submission submission)
       (build-submission user)
       (json-util/log-json "job")
       (submit-job user submission)))

(defn submit-step
  [user submission]
  (let [job-step (build-submission user submission)]
    (json-util/log-json "job step" job-step)
    (do-jex-submission job-step)
    (:uuid job-step)))

(defn update-job-status
  [{:keys [external-id] :as job-step} {job-id :id :as job} status end-date]
  (when (jp/status-follows? status (:status job-step))
    (jp/update-job-step job-id external-id status end-date)
    (jp/update-job job-id status end-date)))

(defn get-default-output-name
  [{output-id :output_id :as io-map} {task-id :task_id :as source-step}]
  (ap/get-default-output-name task-id output-id))

(defn get-job-step-status
  [{:keys [external-id]}]
  (when-let [step (jex-events/get-job-state external-id)]
    {:status  (:status step)
     :enddate (:completion_date step)}))

(defn prepare-step
  [user submission]
  (build-submission user submission))
