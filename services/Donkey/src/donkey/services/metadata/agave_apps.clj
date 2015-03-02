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
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.util :as mu]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils]
            [donkey.util.service :as service]
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

(defn- determine-start-time
  [job]
  (or (db/timestamp-from-str (str (:startdate job)))
      (db/now)))

(defn- store-agave-job
  [job-id job submission]
  (jp/save-job {:id                 job-id
                :job-name           (:name job)
                :description        (:description job)
                :app-id             (:app_id job)
                :app-name           (:app_name job)
                :app-description    (:app_details job)
                :app-wiki-url       (:wiki_url job)
                :result-folder-path (:resultfolderid job)
                :start-date         (determine-start-time job)
                :username           (:username current-user)
                :status             (:status job)
                :notify             (:notify job)}
               submission))

(defn- store-job-step
  [job-id job]
  (jp/save-job-step {:job-id          job-id
                     :step-number     1
                     :external-id     (:id job)
                     :start-date      (determine-start-time job)
                     :status          (:status job)
                     :job-type        jp/agave-job-type
                     :app-step-number 1}))

(defn- build-callback-url
  [id]
  (str (assoc (curl/url (config/agave-callback-base) (str id))
         :query "status=${JOB_STATUS}&external-id=${JOB_ID}&end-time=${JOB_END_TIME}")))

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

(defn- submit-job
  [agave-client submission job-id step-number]
  (->> (assoc submission
         :callbackUrl (build-callback-url job-id)
         :job_id      job-id
         :step_number step-number)
       (.submitJob agave-client)
       (validate-job-info)))

(defn submit-agave-job
  [agave-client submission]
  (let [id         (UUID/randomUUID)
        output-dir (ft/build-result-folder-path submission)
        job        (submit-job agave-client
                               (mu/update-submission-result-folder submission output-dir)
                               id 1)
        job        (assoc job
                     :notify (:notify submission false)
                     :name   (:name submission))
        username   (:shortUsername current-user)
        email      (:email current-user)]
    (store-agave-job id job submission)
    (store-job-step id job)
    (dn/send-job-status-update username email (assoc job :id id))
    {:id         (str id)
     :name       (:name job)
     :status     (:status job)
     :start-date (time-utils/millis-from-str (str (:startdate job)))}))

(defn submit-job-step
  [agave-client job-info job-step submission]
  (:id (submit-agave-job agave-client submission (:id job-info) (:step-number job-step))))

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
