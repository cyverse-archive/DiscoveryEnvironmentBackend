(ns metadactyl.service.apps.agave.jobs
  (:use [slingshot.slingshot :only [try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [kameleon.uuids :as uuids]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]
            [schema.core :as s]))

(defn- build-callback-url
  [id]
  (str (assoc (curl/url (config/agave-callback-base) (str id))
         :query "status=${JOB_STATUS}&external-id=${JOB_ID}&end-time=${JOB_END_TIME}")))

(defn- format-submission
  [agave job-id result-folder-path submission]
  (->> (assoc (dissoc submission :starting_step)
         :callbackUrl          (build-callback-url job-id)
         :job_id               job-id
         :step_number          (:starting_step submission 1)
         :output_dir           result-folder-path
         :create_output_subdir false)))

(defn prepare-submission
  [agave submission]
  (format-submission agave
                     (uuids/uuid)
                     (ft/build-result-folder-path submission)
                     submission))

(def NonBlankString
  (s/both s/Str (s/pred (complement string/blank?) 'non-blank?)))

(def JobInfo
  "A schema used to validate job information."
  {:id              NonBlankString
   :app_id          NonBlankString
   :app_description s/Str
   :app_name        NonBlankString
   :app-disabled    s/Bool
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

(defn- send-submission*
  [agave submission job]
  (assoc (.submitJob agave job)
    :notify (:notify submission false)
    :name   (:name submission)))

;; TODO: implement me!
(defn- store-agave-job
  [id job submission])

;; TODO: implement me!
(defn- store-job-step
  [id job])

;; TODO: implement me!
(defn- format-job-submission-response
  [submission job])

(defn send-submission
  [agave {id :job_id :as submission} job]
  (let [job (send-submission* agave submission job)]
    (store-agave-job id job submission)
    (store-job-step id job)
    (format-job-submission-response submission job)))
