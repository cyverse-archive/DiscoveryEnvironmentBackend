(ns donkey.services.metadata.de-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.osm :as osm]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.property-values :as property-values]
            [donkey.services.metadata.util :as mu]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils])
  (:import [java.util UUID]))

(defn- get-end-date
  [{:keys [status completion_date now_date]}]
  (case status
    mu/failed-status    (db/timestamp-from-str now_date)
    mu/completed-status (db/timestamp-from-str completion_date)
    nil))

(defn- store-submitted-de-job
  [job-id job submission]
  (jp/save-job {:id                 job-id
                :job-name           (:name job)
                :description        (:description job)
                :app-id             (:analysis_id job)
                :app-name           (:analysis_name job)
                :app-description    (:analysis_details job)
                :app-wiki-url       (:wiki_url job)
                :result-folder-path (:resultfolderid job)
                :start-date         (db/timestamp-from-str (str (:startdate job)))
                :username           (:username current-user)
                :status             (:status job)}))

(defn- store-job-step
  [job-id job]
  (jp/save-job-step {:job-id          job-id
                     :step-number     1
                     :external-id     (:id job)
                     :start-date      (db/timestamp-from-str (str (:startdate job)))
                     :status          (:status job)
                     :job-type        jp/de-job-type
                     :app-step-number 1}))

(defn submit-job
  [workspace-id submission]
  (let [job-id     (UUID/randomUUID)
        submission (assoc submission :uuid (str job-id))
        job        (metadactyl/submit-job workspace-id submission)]
    (store-submitted-de-job job-id job submission)
    (store-job-step job-id job)
    {:id         (str job-id)
     :name       (:name job)
     :status     (:status job)
     :start-date (time-utils/millis-from-str (str (:startdate job)))}))

(defn submit-job-step
  [workspace-id job-info job-step submission]
  (->> (assoc submission :uuid (str (:id job-info)))
       (metadactyl/submit-job workspace-id)
       (:id)))

(defn load-de-job-states
  [de-jobs]
  (if-not (empty? de-jobs)
    (->> (osm/get-jobs (map :id de-jobs))
         (map (juxt :uuid identity))
         (into {}))
    {}))

(defn load-app-details
  [ids]
  (into {} (map (juxt :id identity)
                (ap/load-app-details ids))))

;; TODO: implement me
(defn sync-de-job-status
  [job])

(defn update-job-status
  "Updates the status of a job. If this function is called then Agave jobs are disabled, so
   there will always be only one job step."
  [username job job-step status end-time]
  (jp/update-job-step (:id job) (:external-id job-step) status end-time)
  (jp/update-job (:id job) status end-time))

(defn send-job-status-notification
  "This function currently does nothing because job status update notifications are currently
   handled by the notification agent directly."
  [job job-step status end-time])
