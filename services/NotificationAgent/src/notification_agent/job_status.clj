(ns notification-agent.job-status
  (:use [clojure.string :only [blank? lower-case]]
        [notification-agent.config]
        [notification-agent.common]
        [notification-agent.messages]
        [notification-agent.time])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.osm :as osm]
            [clojure.tools.logging :as log]
            [notification-agent.app-db :as app-db]
            [notification-agent.db :as db]))

(defn- get-descriptive-job-name
  "Extracts a descriptive job name from the job state object.  We can count on
   a useful job description for data notifications, so the job description will
   be used for them.  For other types of notifications the name of the job is
   the best candidate."
  [state]
  (if (= (:type state) "data")
    (:description state)
    (first (remove blank? [(:display_name state) (:name state)]))))

(defn- job-status-msg
  "Formats the status message for a job whose status has changed."
  [state]
  (str (get-descriptive-job-name state) " " (lower-case (:status state))))

(defn- job-completed?
  "Determines if a job has completed."
  [state]
  (re-matches #"(?i)\s*(?:completed|failed)\s*" (:status state)))

(defn- format-email-request
  "Formats an e-mail request that can be sent to the iPlant e-mail service."
  [email state]
  {:to        email
   :template  (email-template)
   :subject   (str (get-descriptive-job-name state) " status changed.")
   :from-addr (email-from-address)
   :from-name (email-from-name)
   :values    {:analysisname          (:name state)
               :analysisstatus        (:status state)
               :analysisstartdate     (format-timestamp (:submission_date state))
               :analysisresultsfolder (:output_dir state)
               :analysisdescription   (:description state)}})

(defn- email-requested
  "Determines if e-mail notifications were requested for a job.  The 'notify'
   element in the job state indicates whether or not e-mail notifications were
   requested, which is the case if the 'notify' element is both present and
   true."
  [state]
  (:notify state false))

(defn- add-email-request
  "Includes an e-mail request in a notificaiton message if e-mail
   notifications were requested."
  [msg {addr :email :as state}]
  (if (and (email-enabled) (email-requested state) (valid-email-addr addr))
    (assoc msg :email_request (format-email-request addr state))
    msg))

(defn- state-to-msg
  "Converts an object representing a job state to a notification message."
  [state]
  {:type           (:type state)
   :user           (:user state)
   :outputDir      (:output_dir state)
   :outputManifest (:output_manifest state)
   :message        {:id        ""
                    :timestamp (str (System/currentTimeMillis))
                    :text      (job-status-msg state)}
   :payload        {:id             (:uuid state)
                    :action         "job_status_change"
                    :status         (:status state)
                    :resultfolderid (:output_dir state)
                    :user           (:user state)
                    :name           (:name state "")
                    :display_name   (or (:display_name state) (:name state ""))
                    :startdate      (:submission_date state "")
                    :enddate        (:completion_date state "")
                    :analysis_id    (:analysis_id state "")
                    :analysis_name  (:analysis_name state "")
                    :description    (:description state "")}})

(defn- handle-completed-job
  "Handles a job status update request for a job that has completed and
   returns the state object."
  [state]
  (log/debug "job" (:uuid state) "just completed")
  (persist-and-send-msg (add-email-request (state-to-msg state) state)))

(defn- update-state-fields
  "Updates fields in the state that can be overridden in the DE database."
  [state]
  (let [job-info (app-db/get-job-info (:uuid state))]
    (assoc state
      :name        (or (:name job-info) (:name state))
      :description (or (:description job-info) (:description state)))))

(defn- handle-status-change
  "Handles a job with a status that has been changed since the job was last
   seen by the notification agent.  To do this, a notification needs to be
   generated and the prevous_status field has to be updated with the last
   status that was seen by the notification agent."
  [state]
  (log/debug "the status of job" (:uuid state) "changed")
  (let [state (update-state-fields state)]
    (if (job-completed? state)
      (handle-completed-job state)
      (persist-and-send-msg (state-to-msg state)))
    (db/update-notification-status (:uuid state) (:status state))))

(defn- job-status-changed?
  "Determines whether or not the status of a job corresponding to a state
   object has changed since the last time the notification agent saw the job."
  [{:keys [status uuid]}]
  (and status uuid (not= status (db/get-notification-status uuid))))

(defn- get-jobs-with-inconsistent-state
  "Gets a list of jobs whose current status doesn't match the status last seen
   by the notification agent (which is stored in the misnamed previous_status
   field)."
  []
  (log/debug "retrieving the list of jobs that have been updated while the "
             "notification agent was down")
  (let [jobs (:objects (cheshire/decode (osm/query (jobs-osm) {}) true))]
    (filter #(job-status-changed? (:state %)) jobs)))

(defn- fix-job-status
  "Fixes the status of a job with an inconsistent state.  This function is
   basically just a wrapper around handle-status-change that adds some
   exception handling."
  [state]
  (when-not (nil? (:uuid state))
    (log/debug "fixing state for job" (:uuid state))
    (try (handle-status-change state)
         (catch Throwable t
           (log/warn t "unable to fix status for job" (:uuid state))))))

(defn- fix-inconsistent-state
  "Processes the status changes for any jobs whose state changed without the
   notification agent knowing about it.  This may happen if the system is
   misconfigured or if the notification agent goes down for a while."
  []
  (dorun (map (comp fix-job-status :state)
              (get-jobs-with-inconsistent-state))))

(defn initialize-job-status-service
  "Performs any tasks required to initialize the job status service."
  []
  (try
    (fix-inconsistent-state)
    (catch Exception e
      (log/error e "unable to initialize job status service"))))

(defn handle-job-status
  "Handles a job status update request with the given body."
  [body]
  (let [{:keys [state]} (parse-body body)]
    (log/info "received a job status update request for job" (:uuid state)
              "with status" (:status state))
    (if (job-status-changed? state)
      (handle-status-change state)
      (log/debug "the status of job" (:uuid state) "did not change"))
    (success-resp)))
