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
            [donkey.util.config :as config]
            [donkey.util.db :as db]
            [donkey.util.time :as time-utils]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(defn load-app-details
  [agave app-ids]
  (->> (.listApps agave)
       (:templates)
       (filter (comp (set app-ids) :id))
       (map (juxt :id identity))
       (into {})))

(def ^:private agave-job-validation-map
  "The validation map to use for Agave jobs."
  {:name          string?
   :analysis_name string?
   :id            string?
   :startdate     string?
   :status        string?})

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
  (let [id     (UUID/randomUUID)
        cb-url (build-callback-url id)
        job    (.submitJob agave-client (assoc submission :callbackUrl cb-url))]
    (store-agave-job id job submission)
    (store-job-step id job)
    (dn/send-agave-job-status-update (:shortUsername current-user) (assoc job :id id))
    {:id         (str id)
     :name       (:name job)
     :status     (:status job)
     :start-date (time-utils/millis-from-str (str (:startdate job)))}))

(defn submit-job-step
  [agave-client job-info job-step submission]
  (let [cb-url (build-callback-url (:id job-info))]
     (:id (.submitJob agave-client (assoc submission :callbackUrl cb-url)))))

(defn- determine-display-timestamp
  [k job state]
  (cond (not (string/blank? (k state))) (k state)
        (not (nil? (k job)))            (str (.getTime (k job)))
        :else                           "0"))

(defn format-agave-job
  [agave-apps {app-id :analysis_id :as job}]
  (assoc job
    :startdate    (str (or (db/millis-from-timestamp (:startdate job)) 0))
    :enddate      (str (or (db/millis-from-timestamp (:enddate job)) 0))
    :app_disabled (get-in agave-apps [app-id :disabled] true)))

(defn load-agave-job-states
  [agave agave-jobs]
  (if-not (empty? agave-jobs)
    (try+
     (->> (.listJobs agave (map :id agave-jobs))
          (map (juxt :id identity))
          (into {}))
     (catch [:error_code ce/ERR_UNAVAILABLE] _ {}))
    {}))

(defn get-agave-job
  [agave id not-found-fn]
  (try+
   (not-empty (.listJob agave id))
   (catch [:status 404] _ (not-found-fn id))
   (catch [:status 400] _ (not-found-fn id))
   (catch Object _ (service/request-failure "lookup for HPC job" id))))

(def ^:private complete-status "Completed")
(def ^:private failed-status "Failed")

(defn- is-complete?
  [status]
  (#{failed-status complete-status} status))

(defn- get-job-status
  [step-number max-step-number prev-status status]
  (let [first-step? (= step-number 1)
        last-step?  (= step-number max-step-number)
        failed?     (= status failed-status)
        complete?   (is-complete? status)]
    (cond failed?                           failed-status
          (and first-step? (not complete?)) status
          (and last-step? complete?)        status
          :else                             prev-status)))

(defn- update-job-step-status
  [{:keys [job-id external-id status]} new-status end-time]
  (when-not (= status new-status)
    (jp/update-job-step job-id external-id new-status end-time)))

(defn- update-job-status
  [username {:keys [startdate] :as job} {:keys [step-number]} max-step-number status end-time]
  (let [prev-status  (:status job)
        job-status   (get-job-status step-number max-step-number prev-status status)
        end-time     (when (is-complete? job-status) end-time)
        end-millis   (when-not (nil? end-time) (str (.getTime end-time)))
        start-millis (when-not (nil? startdate) (str (.getTime startdate)))]
    (when-not (= job-status prev-status)
      (jp/update-job (:id job) job-status end-time)
      (dn/send-agave-job-status-update username (assoc job
                                                  :status    job-status
                                                  :enddate   end-millis
                                                  :startdate start-millis)))))

(defn update-agave-job-status
  [agave username job job-step max-step-number status end-time]
  (let [status       (.translateJobStatus agave status)
        username     (string/replace username #"@.*" "")
        end-time     (when (is-complete? status) (db/timestamp-from-str end-time))]
    (update-job-step-status job-step status end-time)
    (update-job-status username job job-step max-step-number status end-time)))

(defn- agave-job-status-changed
  [job curr-state]
  (or (nil? curr-state)
      (not= (:status job) (:status curr-state))
      ((complement string/blank?) (:enddate curr-state))))

(defn sync-agave-job-status
  [agave job]
  (let [curr-state (get-agave-job agave (:external_id job) (constantly nil))]
    (when (agave-job-status-changed job curr-state)
      (jp/update-job
       (:id job)
       {:status     (:status curr-state)
        :start-date (db/timestamp-from-str (str (:startdate curr-state)))
        :end-date   (db/timestamp-from-str (str (:enddate curr-state)))
        :deleted    (nil? curr-state)}))))

(defn get-agave-app-rerun-info
  [agave job-id]
  (let [external-id (:external_id (jp/get-job-by-id job-id))]
    (service/assert-found (.getAppRerunInfo agave external-id) "HPC job" external-id)))

(defn get-agave-job-params
  [agave job-id]
  (let [external-id (:external_id (jp/get-job-by-id job-id))]
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
