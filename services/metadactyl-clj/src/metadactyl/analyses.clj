(ns metadactyl.analyses
  (:use [kameleon.jobs :only [get-job-type-id save-job save-job-step]]
        [kameleon.queries :only [get-user-id]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [korma.core]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.analyses.base :as ab]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.util.config :as config]))

(defn- do-jex-submission
  [job]
  (try+
    (http/post (config/jex-base-url)
               {:body         (cheshire/encode job)
                :content-type :json})
    (catch Object o
      (log/error (:throwable &throw-context) "job submission failed")
      (throw+ {:error_code ce/ERR_REQUEST_FAILED}))))

(defn- store-submitted-job
  "Saves information about a job in the database."
  [job submission]
  (let [job-info {:job_name           (:name job)
                  :job_description    (:description job)
                  :app_id             (:app_id job)
                  :app_name           (:app_name job)
                  :app_description    (:app_description job)
                  :app_wiki_url       (:wiki_url job)
                  :result_folder_path (:output_dir job)
                  :start_date         (sqlfn now)
                  :user_id            (get-user-id (:username current-user))
                  :status             "Submitted"}]
    (save-job job-info (cheshire/encode submission))))

(defn- store-job-step
  "Saves a single job step in the database."
  [job-id job]
  (save-job-step {:job_id          job-id
                  :step_number     1
                  :external_id     (:uuid job)
                  :start_date      (sqlfn now)
                  :status          "Submitted"
                  :job_type_id     (get-job-type-id "DE")
                  :app_step_number 1}))

(defn- save-job-submission
  "Saves a DE job and its job-step in the database."
  [job submission]
  (transaction
    (let [job-id (:id (store-submitted-job job submission))]
      (store-job-step job-id job))))

(defn- submit-job
  [submission job]
  (let [de-only-job? (zero? (ap/count-external-steps (:app_id job)))]
    (do-jex-submission job)
    (if de-only-job?
      (save-job-submission job submission)))
  job)

(defn submit
  [{:keys [user email]} submission]
  (->> (ab/build-submission user email submission)
       (remove-nil-vals)
       (submit-job submission)))
