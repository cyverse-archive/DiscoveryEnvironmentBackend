(ns metadactyl.clients.notifications
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [metadactyl.util.config :as config]))

(defn notificationagent-url
  [& components]
  (str (apply curl/url (config/notification-agent-base) components)))

(defn format-timestamp
  "Formats a timestamp in a standard format."
  [timestamp]
  (if-not (or (string/blank? timestamp) (= "0" timestamp))
    (tf/unparse (:date-time tf/formatters) (tc/from-long (Long/parseLong timestamp)))
    ""))

(defn- send-notification
  "Sends a notification to a user."
  [m]
  (http/post (notificationagent-url "notification")
               {:content-type :json
                :body (cheshire/encode m)}))

(defn- send-email?
  [job-info]
  (and (:notify job-info false)
       (#{"Completed" "Failed"} (:status job-info))))

(defn- format-job-status-update
  "Formats a job status update notification to send to the notification agent."
  [username email-address {job-name :name :as job-info}]
  {:type           "analysis"
   :user           username
   :subject        (str job-name " status changed.")
   :message        (str job-name " " (string/lower-case (:status job-info)))
   :email          (send-email? job-info)
   :email_template "analysis_status_change"
   :payload        (assoc job-info
                     :analysisname          (:name job-info)
                     :analysisdescription   (:description job-info)
                     :analysisstatus        (:status job-info)
                     :analysisstartdate     (format-timestamp (:startdate job-info))
                     :analysisresultsfolder (:resultfolderid job-info)
                     :email_address         email-address
                     :action                "job_status_change"
                     :user                  username)})

(defn send-job-status-update
  "Sends notification of an Agave or DE job status update to the user."
  [username email-address job-info]
  (try
    (send-notification (format-job-status-update username email-address job-info))
    (catch Exception e
      (log/warn e "unable to send job status update notification for" (:id job-info)))))
