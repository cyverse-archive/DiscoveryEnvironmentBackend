(ns mescal.agave-de-v2.jobs
  (:use [clojure.java.io :only [file]]
        [medley.core :only [remove-vals]])
  (:require [clojure.string :as string]
            [mescal.agave-de-v2.app-listings :as app-listings]
            [mescal.util :as util]))

(defn- params-for
  ([config app-section]
     (params-for config app-section identity))
  ([config app-section preprocessing-fn]
     (let [get-param-val (comp preprocessing-fn config)]
       (->> (map (comp keyword :id) app-section)
            (map (juxt identity get-param-val))
            (into {})
            (remove-vals nil?)))))

(defn- prepare-params
  [agave app config]
  {:inputs     (params-for config (app :inputs) #(.agaveUrl agave %))
   :parameters (params-for config (app :parameters))})

(defn- archive-path
  [agave {job-name :name output-directory :outputDirectory}]
  (.agaveFilePath agave (str output-directory "/" (string/replace job-name #"\s" "_"))))

(def ^:private submitted "Submitted")
(def ^:private running "Running")
(def ^:private failed "Failed")
(def ^:private completed "Completed")

(def ^:private job-status-translations
  {"PENDING"            submitted
   "STAGING_INPUTS"     submitted
   "CLEANING_UP"        running
   "ARCHIVING"          running
   "STAGING_JOB"        submitted
   "FINISHED"           completed
   "KILLED"             failed
   "FAILED"             failed
   "STOPPED"            failed
   "RUNNING"            running
   "PAUSED"             running
   "QUEUED"             submitted
   "SUBMITTING"         submitted
   "STAGED"             submitted
   "PROCESSING_INPUTS"  submitted
   "ARCHIVING_FINISHED" completed
   "ARCHIVING_FAILED"   failed})

(defn- job-notifications
  [callback-url]
  (map (fn [status] {:url callback-url :event status}) (keys job-status-translations)))

(defn prepare-submission
  [agave app submission]
  (->> (assoc (prepare-params agave app (:config submission))
         :name          (:name submission)
         :appId         (:analysis_id submission)
         :archive       true
         :archivePath   (archive-path agave submission)
         :archiveSystem (.storageSystem agave)
         :notifications (job-notifications (:callbackUrl submission)))
       (remove-vals nil?)))

(defn- app-enabled?
  [statuses jobs-enabled? listing]
  (and jobs-enabled?
       (:available listing)
       (= "up" (statuses (:executionHost listing)))))

(defn- get-result-folder-id
  [agave job]
  (when-let [agave-path (or (:archivePath job) (get-in job [:_links :archiveData :href]))]
    (.irodsFilePath agave agave-path)))

(defn format-job
  ([agave jobs-enabled? app-info-map job]
     (let [app-id   (:appId job)
           app-info (app-info-map app-id {})]
       {:id               (str (:id job))
        :analysis_id      app-id
        :analysis_details (:shortDescription app-info "")
        :analysis_name    (app-listings/get-app-name app-info)
        :description      ""
        :enddate          (or (str (util/parse-timestamp (:endTime job))) "")
        :name             (:name job)
        :raw_status       (:status job)
        :resultfolderid   (get-result-folder-id agave job)
        :startdate        (or (str (util/parse-timestamp (:startTime job))) "")
        :status           (job-status-translations (:status job) "")
        :wiki_url         ""}))
  ([agave jobs-enabled? statuses app-info-map job]
     (let [app-id   (:appId job)
           app-info (app-info-map app-id {})]
       (assoc (format-job agave jobs-enabled? app-info-map job)
         :app-disabled (not (app-enabled? statuses jobs-enabled? app-info))))))

(defn translate-job-status
  [status]
  (get job-status-translations status))
