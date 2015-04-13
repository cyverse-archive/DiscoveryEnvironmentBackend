(ns donkey.services.metadata.agave-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.util :as mu]
            [donkey.util.config :as config]
            [donkey.util.service :as service]
            [kameleon.db :as db]
            [schema.core :as s])
  (:import [java.util UUID]))

(defn- submit-job
  [submission job-id step-number]
  (->> (assoc submission
         :job_id        job-id
         :starting_step step-number)
       (metadactyl/submit-job)))

(defn submit-job-step
  [agave-client job-info job-step submission]
  (:id (submit-job agave-client submission (:id job-info) (:step-number job-step))))

(defn get-agave-app-rerun-info
  [agave {:keys [external-id]}]
  (service/assert-found (.getAppRerunInfo agave external-id) "HPC job" external-id))

(defn get-agave-job-params
  [agave {:keys [external-id]}]
  (service/assert-found (.getJobParams agave external-id) "HPC job" external-id))

(defn- get-agave-task
  [agave external-app-id]
  ((comp first :tasks)
   (service/assert-found (.listAppTasks agave external-app-id) "Agave app" external-app-id)))

(defn- format-task
  [agave external-app-ids {:keys [id] :as task}]
  (if-let [external-app-id (external-app-ids id)]
    (merge task (select-keys (get-agave-task agave external-app-id) [:inputs :outputs]))
    task))

(defn format-pipeline-tasks
  [agave pipeline]
  (let [external-app-ids (into {} (map (juxt :task_id :external_app_id) (:steps pipeline)))]
    (update-in pipeline [:tasks] (partial map (partial format-task agave external-app-ids)))))
