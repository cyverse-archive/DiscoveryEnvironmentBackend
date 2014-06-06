(ns donkey.services.metadata.agave-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [try+]])
  (:require [cemerick.url :as curl]
            [clj-jargon.item-info :as jargon-info]
            [clj-jargon.init :as jargon-init]
            [clj-jargon.permissions :as jargon-perms]
            [clojure.string :as string]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.jobs :as jp]
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(def ^:private agave-job-validation-map
  "The validation map to use for Agave jobs."
  {:name          string?
   :analysis_name string?
   :id            string?
   :startdate     string?
   :status        string?})

(defn- store-agave-job
  [agave id job]
  (validate-map job agave-job-validation-map)
  (jp/save-job (:id job) (:name job) jp/agave-job-type (:username current-user) (:status job)
               :id          id
               :description (:description job)
               :app-name    (:analysis_name job)
               :start-date  (db/timestamp-from-str (str (:startdate job)))
               :end-date    (db/timestamp-from-str (str (:enddate job)))))

(defn populate-job-descriptions
  [agave-client username]
  (->> (jp/list-jobs-with-null-descriptions username [jp/agave-job-type])
       (map :id)
       (.listJobs agave-client)
       (map (juxt :id :description))
       (map jp/set-job-description)
       (dorun)))

(defn- build-callback-url
  [id]
  (str (assoc (curl/url (config/agave-callback-base) (str id))
         :query "status=${JOB_STATUS}&job=${JOB_ID}&end-time=${JOB_END_TIME}")))

(defn submit-agave-job
  [agave-client submission]
  (let [id     (UUID/randomUUID)
        cb-url (build-callback-url id)
        job    (.submitJob agave-client (assoc submission :callbackUrl cb-url))]
    (store-agave-job agave-client id job)
    (dn/send-agave-job-status-update (:shortUsername current-user) job)
    {:id id
     :name (:name job)
     :status (:status job)
     :start-date (time-utils/millis-from-str (str (:startdate job)))}))

(defn format-agave-job
  [job state]
  (assoc state
    :id            (:id job)
    :description   (or (:description job) (:description state))
    :startdate     (str (or (db/millis-from-timestamp (:startdate job)) 0))
    :enddate       (str (or (db/millis-from-timestamp (:enddate job)) 0))
    :analysis_name (:analysis_name job)
    :status        (:status job)))

(defn load-agave-job-states
  [agave agave-jobs]
  (if-not (empty? agave-jobs)
    (->> (.listJobs agave (map :id agave-jobs))
         (map (juxt :id identity))
         (into {}))
    {}))

(defn get-agave-job
  [agave id not-found-fn]
  (try+
   (not-empty (.listRawJob agave id))
   (catch [:status 404] _ (not-found-fn id))
   (catch [:status 400] _ (not-found-fn id))
   (catch Object _ (service/request-failure "lookup for HPC job" id))))

(defn- is-complete?
  [status]
  (#{"Failed" "Completed"} status))

(defn update-agave-job-status
  [agave username {:keys [startdate] :as prev-job-info} status end-time]
  (let [status       (.translateJobStatus agave status)
        username     (string/replace username #"@.*" "")
        end-time     (when (is-complete? status) (db/timestamp-from-str end-time))
        end-millis   (when-not (nil? end-time) (str (.getTime end-time)))
        start-millis (str (.getTime startdate))]
    (when-not (= status (:status prev-job-info))
      (jp/update-job (:id prev-job-info) status end-time)
      (dn/send-agave-job-status-update username (assoc prev-job-info
                                                  :status    status
                                                  :enddate   end-millis
                                                  :startdate start-millis)))))

(defn remove-deleted-agave-jobs
  "Marks jobs that have been deleted in Agave as deleted in the DE also."
  [agave]
  (let [extant-jobs (set (.listJobIds agave))]
    (->> (jp/get-external-job-ids (:username current-user) {:job-types [jp/agave-job-type]})
         (remove extant-jobs)
         (map #(jp/update-job % {:deleted true}))
         (dorun))))

(defn- agave-job-status-changed
  [job curr-state]
  (or (nil? curr-state)
      (not= (:status job) (:status curr-state))
      ((complement string/blank?) (:enddate curr-state))))

(defn sync-agave-job-status
  [agave job]
  (let [curr-state (get-agave-job agave (:external_id job) (constantly nil))]
    (when (agave-job-status-changed job curr-state)
      (jp/update-job-by-internal-id
       (:id job)
       {:status   (:status curr-state)
        :end-date (db/timestamp-from-str (str (:enddate curr-state)))
        :deleted  (nil? curr-state)}))))

(defn get-agave-app-rerun-info
  [agave job-id]
  (.getAppRerunInfo agave job-id))

(defn get-agave-job-params
  [agave job-id]
  (.getJobParams agave job-id))

(defn- is-readable?
  [cm path]
  (and (jargon-info/exists? cm path)
       (jargon-perms/is-readable? cm (:shortUsername current-user) path)))

(defn- is-input-property
  [{property-type :type}]
  (re-matches #".*Input" property-type))
