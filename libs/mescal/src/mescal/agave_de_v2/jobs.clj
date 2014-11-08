(ns mescal.agave-de-v2.jobs
  (:use [clojure.java.io :only [file]]
        [medley.core :only [remove-vals]])
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [mescal.agave-de-v2.app-listings :as app-listings]
            [mescal.agave-de-v2.job-params :as params]
            [mescal.util :as util]))

(def ^:private timestamp-formatter
  (tf/formatter "yyyy-MM-dd-HH-mm-ss.S"))

(defn- add-param-prefix
  [prefix param]
  (if-not (string/blank? (str prefix))
    (keyword (str prefix "_" (name param)))
    param))

(defn- params-for
  ([config param-prefix app-section]
     (params-for config param-prefix app-section identity))
  ([config param-prefix app-section preprocessing-fn]
     (let [get-param-val (comp preprocessing-fn config (partial add-param-prefix param-prefix))]
       (->> (map (comp keyword :id) app-section)
            (map (juxt identity get-param-val))
            (into {})
            (remove-vals nil?)))))

(defn- prepare-params
  [agave app param-prefix config]
  {:inputs     (params-for config param-prefix (app :inputs) #(.agaveUrl agave %))
   :parameters (params-for config param-prefix (app :parameters) #(if (map? %) (:value %) %))})

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
  [{:url        callback-url
    :event      "*"
    :persistent true}])

(defn prepare-submission
  [agave app submission]
  (->> (assoc (prepare-params agave app (:paramPrefix submission) (:config submission))
         :name          (:name submission)
         :appId         (:app_id submission)
         :archive       true
         :archivePath   (.agaveFilePath agave (:outputDirectory submission))
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
       {:id              (str (:id job))
        :app_id          app-id
        :app_description (:shortDescription app-info "")
        :app_name        (app-listings/get-app-name app-info)
        :description     ""
        :enddate         (or (str (util/parse-timestamp (:endTime job))) "")
        :name            (:name job)
        :raw_status      (:status job)
        :resultfolderid  (get-result-folder-id agave job)
        :startdate       (or (str (util/parse-timestamp (:startTime job))) "")
        :status          (job-status-translations (:status job) "")
        :wiki_url        ""}))
  ([agave jobs-enabled? statuses app-info-map job]
     (let [app-id   (:appId job)
           app-info (app-info-map app-id {})]
       (assoc (format-job agave jobs-enabled? app-info-map job)
         :app-disabled (not (app-enabled? statuses jobs-enabled? app-info))))))

(defn translate-job-status
  [status]
  (get job-status-translations status))

(defn regenerate-job-submission
  [agave job]
  (let [app-id     (:appId job)
        app        (.getApp agave app-id)
        job-params (:parameters (params/format-params agave job app-id app))
        cfg-entry  (juxt (comp keyword :param_id) (comp :value :param_value))]
    {:app_id               app-id
     :name                 (:name job)
     :debug                false
     :notify               false
     :output_dir           (get-result-folder-id agave job)
     :create_output_subdir true
     :description          ""
     :config               (into {} (map cfg-entry job-params))}))
