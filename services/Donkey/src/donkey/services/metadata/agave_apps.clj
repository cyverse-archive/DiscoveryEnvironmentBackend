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

(defn- app-sorter
  [sort-field sort-dir]
  (partial sort-by
           (keyword sort-field)
           (if (and sort-dir (= (string/upper-case sort-dir) "DESC"))
             #(compare %2 %1)
             #(compare %1 %2))))

(defn- sort-apps
  [res {:keys [sort-field sort-dir]}]
  (if sort-field
    (update-in res [:apps] (app-sorter sort-field sort-dir))
    res))

(defn- apply-offset
  [res params]
  (let [offset (service/string->long (:offset params "0"))]
    (if (pos? offset)
      (update-in res [:apps] (partial drop offset))
      res)))

(defn- apply-limit
  [res params]
  (let [limit (service/string->long (:limit params "0"))]
    (if (pos? limit)
      (update-in res [:apps] (partial take limit))
      res)))

(defn list-apps
  [agave category-id params]
  (-> (.listApps agave)
      (sort-apps params)
      (apply-offset params)
      (apply-limit params)))

(defn load-app-details
  [agave]
  (try+
   (->> (.listApps agave)
        (:apps)
        (map (juxt :id identity))
        (into {}))
   (catch [:error_code ce/ERR_UNAVAILABLE] _
     {})))

(defn- submit-job
  [submission job-id step-number]
  (->> (assoc submission
         :job_id        job-id
         :starting_step step-number)
       (metadactyl/submit-job)))

(defn- format-job-submission-response
  [job-info]
  {:id         (:id job-info)
   :name       (:name job-info)
   :status     (:status job-info)
   :start-date (:db/millis-from-str (str (:startdate job-info)))})

(defn submit-agave-job
  [submission]
  (let [job-info (submit-job submission (UUID/randomUUID) 1)]
    (dn/send-job-status-update (:shortUsername current-user) (:email current-user) job-info)
    (format-job-submission-response job-info)))

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
