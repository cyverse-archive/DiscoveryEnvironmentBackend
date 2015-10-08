(ns metadactyl.service.apps.agave.jobs
  (:use [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.util.config :as config]
            [metadactyl.util.json :as json-util]
            [metadactyl.util.service :as service]
            [schema.core :as s]))

(defn- build-callback-url
  [id]
  (str (assoc (curl/url (config/agave-callback-base) (str id))
         :query "status=${JOB_STATUS}&external-id=${JOB_ID}&end-time=${JOB_END_TIME}")))

(defn- format-submission
  [agave job-id result-folder-path submission]
  (->> (assoc (dissoc submission :starting_step :step_number)
         :callbackUrl          (build-callback-url job-id)
         :job_id               job-id
         :step_number          (:step_number submission 1)
         :output_dir           result-folder-path
         :create_output_subdir false)))

(defn- prepare-submission
  [agave job-id submission]
  (->> (format-submission agave
                          job-id
                          (ft/build-result-folder-path submission)
                          submission)
       (.prepareJobSubmission agave)))

(def NonBlankString
  (s/both s/Str (s/pred (complement string/blank?) 'non-blank?)))

(def JobInfo
  "A schema used to validate job information."
  {:id              NonBlankString
   :app_id          NonBlankString
   :app_description s/Str
   :app_name        NonBlankString
   :app_disabled    s/Bool
   :description     s/Str
   :enddate         s/Str
   :name            NonBlankString
   :raw_status      s/Str
   :resultfolderid  NonBlankString
   :startdate       s/Str
   :status          NonBlankString
   :wiki_url        s/Str})

(defn- validate-job-info
  [job-info]
  (try+
   (s/validate JobInfo job-info)
   (catch Object _
     (log/error (:throwable &throw-context)
                (str "received an invalid job submission response from Agave:\n"
                     (cheshire/encode job-info {:pretty true})))
     (service/request-failure (str "Unexpected job submission response: "
                                   (.getMessage (:throwable &throw-context)))))))

(defn- determine-start-time
  [job]
  (or (db/timestamp-from-str (str (:startdate job)))
      (db/now)))

(defn- send-submission*
  [agave user submission job]
  (try+
   (let [job-info (.sendJobSubmission agave job)]
     (assoc job-info
       :name      (:name submission)
       :notify    (:notify submission false)
       :startdate (determine-start-time job)
       :username  (:username user)))
   (catch Object _
     (when-not (:parent_id submission)
       (throw+)))))

(defn- store-agave-job
  [job-id job submission]
  (jp/save-job {:id                 job-id
                :job-name           (:name job)
                :description        (:description submission)
                :app-id             (:app_id job)
                :app-name           (:app_name job)
                :app-description    (:app_details job)
                :app-wiki-url       (:wiki_url job)
                :result-folder-path (:resultfolderid job)
                :start-date         (:startdate job)
                :username           (:username job)
                :status             (:status job)
                :notify             (:notify job)
                :parent-id          (:parent_id submission)}
               submission))

(defn- store-job-step
  [job-id job]
  (jp/save-job-step {:job-id          job-id
                     :step-number     1
                     :external-id     (:id job)
                     :start-date      (:startdate job)
                     :status          (:status job)
                     :job-type        jp/agave-job-type
                     :app-step-number 1}))

(defn- format-job-submission-response
  [job-id submission job]
  (remove-nil-vals
   {:app_description (:app_description job)
    :app_disabled    false
    :app_id          (:app_id job)
    :app_name        (:app_name job)
    :batch           false
    :description     (:description job)
    :enddate         (:enddate job)
    :id              job-id
    :name            (:name job)
    :notify          (:notify job)
    :resultfolderid  (:resultfolderid job)
    :startdate       (str (.getTime (:startdate job)))
    :status          jp/submitted-status
    :username        (:username job)
    :wiki_url        (:wiki_url job)
    :parent_id       (:parent_id submission)}))

(defn- handle-successful-submission
  [job-id job submission]
  (store-agave-job job-id job submission)
  (store-job-step job-id job)
  (format-job-submission-response job-id submission job))

(defn- handle-failed-submission
  [job-id job submission]
  (let [job (assoc job :status jp/failed-status)]
    (store-agave-job job-id job submission)
    (store-job-step job-id job)
    (format-job-submission-response job-id submission job)))

(defn- send-submission
  [agave user job-id submission job]
  (if-let [submitted-job (send-submission* agave user submission job)]
    (handle-successful-submission job-id submitted-job submission)
    (handle-failed-submission job-id job submission)))

(defn submit
  [agave user submission]
  (let [job-id (or (:job_id submission) (uuids/uuid))]
    (->> (prepare-submission agave job-id submission)
         (json-util/log-json "job")
         (send-submission agave user job-id submission))))

(defn submit-step
  [agave job-id submission]
  (->> (prepare-submission agave job-id submission)
       (json-util/log-json "job step")
       (.sendJobSubmission agave)
       (:id)))

(defn- translate-job-status
  [agave status]
  (if-not (jp/valid-status? status)
    (.translateJobStatus agave status)
    status))

(defn update-job-status
  [agave {:keys [external-id] :as job-step} {job-id :id :as job} status end-date]
  (let [status (translate-job-status agave status)]
    (when (and status (jp/status-follows? status (:status job-step)))
      (jp/update-job-step job-id external-id status end-date)
      (jp/update-job job-id status end-date))))

(defn get-default-output-name
  [agave {external-output-id :external_output_id} {external-app-id :external_app_id}]
  (.getDefaultOutputName agave external-app-id external-output-id))

(defn get-job-step-status
  [agave {:keys [external-id]}]
  (try+
   (select-keys (.listJob agave external-id) [:status :enddate])
   (catch [:status 404] _ nil)))

(defn prepare-step-submission
  [agave job-id submission]
  (prepare-submission agave job-id submission))
