(ns donkey.services.metadata.de-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.osm :as osm]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils])
  (:import [java.util UUID]))

(defn- get-end-date
  [{:keys [status completion_date now_date]}]
  (case status
    "Failed"    (db/timestamp-from-str now_date)
    "Completed" (db/timestamp-from-str completion_date)
                nil))

(defn- store-submitted-de-job
  [job-id job]
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
  (let [job-id (UUID/randomUUID)
        job    (metadactyl/submit-job workspace-id submission)]
    (store-submitted-de-job job-id job)
    (store-job-step job-id job)
    {:id         job-id
     :name       (:name job)
     :status     (:status job)
     :start-date (time-utils/millis-from-str (str (:startdate job)))}))

(defn format-de-job
  [states de-apps job]
  (let [state (states (:id job) {})
        app   (de-apps (:analysis_id state) {})]
   (assoc job
     :startdate        (str (or (db/millis-from-timestamp (:startdate job)) 0))
     :enddate          (str (or (db/millis-from-timestamp (:enddate job)) 0))
     :analysis_id      (:analysis_id state)
     :analysis_details (:description app)
     :wiki_url         (:wikiurl app "")
     :app_disabled     (:disabled app false)
     :description      (or (:description job) (:description state))
     :resultfolderid   (:output_dir state))))

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

(defn list-de-jobs
  [limit offset sort-field sort-order filter]
  (let [user    (:username current-user)
        jobs    (jp/list-jobs-of-types user limit offset sort-field sort-order filter
                                       [jp/de-job-type])
        states  (load-de-job-states jobs)
        de-apps (load-app-details (map :analysis_id states))]
    (mapv (partial format-de-job states de-apps) jobs)))

(defn- de-job-status-changed
  [job curr-state]
  (or (not= (:status job) (:status curr-state))
      ((complement nil?) (get-end-date curr-state))
      (:deleted curr-state)))

(defn sync-de-job-status
  [job]
  (let [curr-state (first (osm/get-jobs [(:external_id job)]))]
    (if-not (nil? curr-state)
      (when (de-job-status-changed job curr-state)
        (jp/update-job-by-internal-id
         (:id job)
         {:status   (:status curr-state)
          :end-date (get-end-date curr-state)
          :deleted  (:deleted curr-state false)}))
      (jp/update-job-by-internal-id (:id job) {:deleted true}))))

(defn get-de-job-params
  [job-id]
  (metadactyl/get-property-values job-id))

(defn get-de-app-rerun-info
  [job-id]
  (metadactyl/get-app-rerun-info job-id))
