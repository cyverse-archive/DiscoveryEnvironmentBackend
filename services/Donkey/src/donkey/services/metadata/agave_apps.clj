(ns donkey.services.metadata.agave-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as curl]
            [clj-jargon.item-info :as jargon-info]
            [clj-jargon.init :as jargon-init]
            [clj-jargon.permissions :as jargon-perms]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.util :as mu]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(defn load-app-details
  [agave]
  (->> (.listApps agave)
       (:templates)
       (map (juxt :id identity))
       (into {})))

(defn- determine-start-time
  [job]
  (or (db/timestamp-from-str (str (:startdate job)))
      (db/now)))

(defn- store-agave-job
  [job-id job submission]
  (jp/save-job {:id                 job-id
                :job-name           (:name job)
                :description        (:description job)
                :app-id             (:analysis_id job)
                :app-name           (:analysis_name job)
                :app-description    (:analysis_details job)
                :app-wiki-url       (:wiki_url job)
                :result-folder-path (:resultfolderid job)
                :start-date         (determine-start-time job)
                :username           (:username current-user)
                :status             (:status job)}
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

(defn submit-agave-job
  [agave-client submission]
  (let [id         (UUID/randomUUID)
        cb-url     (build-callback-url id)
        output-dir (mu/build-result-folder-path submission)
        job        (.submitJob agave-client
                               (assoc (mu/update-submission-result-folder submission output-dir)
                                 :callbackUrl cb-url))
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
  (let [cb-url (build-callback-url (:id job-info))]
     (:id (.submitJob agave-client (assoc submission :callbackUrl cb-url)))))

(defn get-agave-app-rerun-info
  [agave job]
  (let [external-id (:external_id job)]
    (service/assert-found (.getAppRerunInfo agave external-id) "HPC job" external-id)))

(defn get-agave-job-params
  [agave job]
  (let [external-id (:external_id job)]
    (service/assert-found (.getJobParams agave external-id) "HPC job" external-id)))

(defn search-apps
  [agave-client search-term def-result]
  (try+
   (.searchApps agave-client search-term)
   (catch [:error_code ce/ERR_UNAVAILABLE] _ def-result)))

(defn- format-workflow-data-objects
  [template]
  (let [fields  [:description :format :id :name :required]
        fmt     (fn [data-obj] (select-keys data-obj fields))
        fmt-all (fn [data-objs] (map (comp fmt :data_object) data-objs))]
    (assoc template
      :inputs  (fmt-all (:inputs template))
      :outputs (fmt-all (:outputs template)))))

(defn- get-workflow-templates
  [agave workflow]
  (->> (mapcat :steps (:analyses workflow))
       (filter (fn [step] (= "External" (:app_type step))))
       (map (fn [step] (.listAppDataObjects agave (:template_id step))))
       (map format-workflow-data-objects)))

(defn add-workflow-templates
  [agave workflow]
  (update-in workflow [:templates] (partial concat (get-workflow-templates agave workflow))))
