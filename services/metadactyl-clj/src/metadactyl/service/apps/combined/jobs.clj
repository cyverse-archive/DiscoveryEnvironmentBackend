(ns metadactyl.service.apps.combined.jobs
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.combined.util :as cu]
            [metadactyl.util.json :as json-util]
            [metadactyl.util.service :as service]))

(defn- is-de-job-step?
  [job-step]
  (= (:job-type job-step) jp/de-job-type))

(defn- app-step-partitioner
  "Partitions app steps into units of execution. Each external app step has to run by itself.
  Consecutive DE app steps can be combined into a single step."
  [{external-app-id :external_app_id step-number :app_step_number}]
  (when-not (nil? external-app-id)
    (str external-app-id "/" step-number)))

(defn- add-job-step-type
  "Determines the type of a job step."
  [job-step]
  (assoc job-step
    :job_type (if (nil? (:external_app_id job-step)) jp/de-job-type jp/agave-job-type)))

(defn- load-job-steps
  "Loads the app steps from the database, grouping consecutive DE steps into a single step."
  [app-id]
  (->> (map (fn [n step] (assoc step :app_step_number n))
            (iterate inc 1)
            (ap/load-app-steps app-id))
       (partition-by app-step-partitioner)
       (map first)
       (map (fn [n step] (assoc step :step_number n))
            (iterate inc 1))
       (map add-job-step-type)))

(defn- validate-job-steps
  "Verifies that at least one step is associated with a job submission."
  [app-id steps]
  (when (empty? steps)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     (str "app " app-id " has no steps")}))
  steps)

(defn- build-job-save-info
  [user result-folder-path job-id app-info submission]
  {:id                 job-id
   :job-name           (:name submission)
   :description        (:description submission)
   :app-id             (:id app-info)
   :app-name           (:name app-info)
   :app-description    (:description app-info)
   :app-wiki-url       (:wiki_url app-info)
   :result-folder-path result-folder-path
   :start-date         (db/now)
   :status             "Submitted"
   :username           (:username user)
   :notify             (:notify submission false)})

(defn- build-job-step-save-info
  [job-id job-step]
  {:job-id          job-id
   :step-number     (:step_number job-step)
   :status          jp/pending-status
   :job-type        (:job_type job-step)
   :app-step-number (:app_step_number job-step)})

(defn- build-job-step-list
  [job-id app-id]
  (->> (load-job-steps app-id)
       (validate-job-steps app-id)
       (map (partial build-job-step-save-info job-id))))

(defn- prepare-de-job-step-submission
  [client job-info job-step submission]
  (.prepareJobSubmission
   client
   (assoc submission
     :create_output_subdir false
     :output_dir           (:result-folder-path job-info)
     :starting_step        (:app-step-number job-step))))

(defn- get-current-app-step
  [{:keys [app-id]} {:keys [app-step-number]}]
  (nth (ap/load-app-steps app-id) (dec app-step-number)))

(defn- prepare-agave-job-step-submission
  [client job-info job-step submission]
  (let [app-step (get-current-app-step job-info job-step)]
    (.prepareJobSubmission
     client
     (assoc submission
       :create_output_subdir false
       :output_dir           (:result-folder-path job-info)
       :app_id               (:external_app_id app-step)
       :paramPrefix          (:step_id app-step)))))

(defn- prepare-job-step-submission
  [client job-info job-step submission]
  (if (is-de-job-step? job-step)
    (prepare-de-job-step-submission client job-info job-step submission)
    (prepare-agave-job-step-submission client job-info job-step submission)))

(defn- record-step-submission
  [job-id step-number external-id]
  (->>  {:external-id external-id
         :status      jp/submitted-status
         :start-date  (db/now)}
        (jp/update-job-step-number job-id step-number)))

(defn- submit-job-step
  [client job-info job-step submission]
  (->> (prepare-job-step-submission client job-info job-step submission)
       (json-util/log-json "job-step")
       (.submitJobStep client)
       (record-step-submission (:id job-info) (:step-number job-step))))

(defn- get-apps-client
  [clients job-step]
  (if (is-de-job-step? job-step)
    (cu/get-apps-client clients jp/de-client-name)
    (cu/get-apps-client clients jp/agave-client-name)))

(defn submit
  [user clients {app-id :app_id :as submission}]
  (let [job-id      (uuids/uuid)
        job-steps   (build-job-step-list job-id app-id)
        app-info    (service/assert-found (ap/load-app-info app-id) "app" app-id)
        job-info    (build-job-save-info user (ft/build-result-folder-path submission)
                                         job-id app-info submission)
        job-step    (first job-steps)]
    (jp/save-multistep-job job-info job-steps submission)
    (submit-job-step (get-apps-client clients job-step) job-info job-step submission)
    job-id))
