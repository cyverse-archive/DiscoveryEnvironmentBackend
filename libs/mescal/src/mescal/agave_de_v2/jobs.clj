(ns mescal.agave-de-v2.jobs
  (:use [clojure.java.io :only [file]]
        [medley.core :only [remove-vals]])
  (:require [clojure.string :as string]
            [mescal.agave-de-v2.app-listings :as app-listings]
            [mescal.util :as util]))

(defn- remove-trailing-slash
  [path]
  (string/replace path #"/$" ""))

(defn- remove-leading-slash
  [path]
  (string/replace path #"^/" ""))

(defn- agave-path
  [irods-home path]
  (when-not (nil? path)
    (string/replace path (re-pattern (str "^\\Q" (remove-trailing-slash irods-home))) "")))

(defn- de-path
  [irods-home path]
  (when-not (nil? path)
    (str (file irods-home (remove-leading-slash path)))))

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
  [irods-home app config]
  {:inputs     (params-for config (app :inputs) (partial agave-path irods-home))
   :parameters (params-for config (app :parameters))})

(defn- archive-path
  [irods-home {job-name :name output-directory :outputDirectory}]
  (agave-path irods-home (str output-directory "/" (string/replace job-name #"\s" "_"))))

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
  [irods-home app submission]
  (->> (assoc (prepare-params irods-home app (:config submission))
         :name          (:name submission)
         :appId         (:analysis_id submission)
         :archive       true
         :archivePath   (archive-path irods-home submission)
         :notifications (job-notifications (:callbackUrl submission)))
       (remove-vals nil?)))

(defn- app-enabled?
  [statuses jobs-enabled? listing]
  (and jobs-enabled?
       (:available listing)
       (= "up" (statuses (:executionHost listing)))))

(defn- build-path
  [base & rest]
  (string/join "/" (cons base (map #(string/replace % #"^/|/$" "") rest))))

(defn format-job
  ([irods-home jobs-enabled? app-info-map job]
     (let [app-id   (:appId job)
           app-info (app-info-map app-id {})]
       {:id               (str (:id job))
        :analysis_id      app-id
        :analysis_details (:longDescription app-info "")
        :analysis_name    (app-listings/get-app-name app-info)
        :description      ""
        :enddate          (or (str (util/parse-timestamp (:endTime job))) "")
        :name             (:name job)
        :raw_status       (:status job)
        :resultfolderid   (build-path irods-home (:archivePath job))
        :startdate        (str (util/parse-timestamp (:submitTime job)))
        :status           (job-status-translations (:status job) "")
        :wiki_url         ""}))
  ([irods-home jobs-enabled? statuses app-info-map job]
     (let [app-id   (:appId job)
           app-info (app-info-map app-id {})]
       (assoc (format-job irods-home jobs-enabled? app-info-map job)
         :app-disabled (not (app-enabled? statuses jobs-enabled? app-info))))))

(defn translate-job-status
  [status]
  (get job-status-translations status))
