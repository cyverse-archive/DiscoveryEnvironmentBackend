(ns donkey.clients.notifications
  (:use [donkey.persistence.tool-requests :only [email-template-for]]
        [donkey.util.config :only [notificationagent-base-url]]
        [donkey.util.service :only [build-url build-url-with-query decode-stream]]
        [donkey.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.util.time :as ut]
            [donkey.util.transformers :as xforms]))

(defn notificationagent-url
  "Builds a URL that can be used to connect to the notification agent."
  ([relative-url]
     (notificationagent-url relative-url {}))
  ([relative-url query]
     (build-url-with-query (notificationagent-base-url)
                           (add-current-user-to-map query) relative-url)))

(defn send-notification
  "Sends a notification to a user."
  [m]
  (let [res (client/post (notificationagent-url "notification")
                         {:content-type :json
                          :body (cheshire/encode m)})]
    res))

(defn send-tool-notification
  "Sends notification of tool deployment to a user if notification information
   is included in the import JSON."
  [m]
  (let [{:keys [user email]} m]
    (when (every? (comp not nil?) [user email])
      (try
        (send-notification {:type "tool"
                            :user user
                            :subject (str (:name m) " has been deployed")
                            :email true
                            :email_template "tool_deployment"
                            :payload {:email_address email
                                      :toolname (:name m)
                                      :tooldirectory (:location m)
                                      :tooldescription (:description m)
                                      :toolattribution (:attribution m)
                                      :toolversion (:version m)}})
        (catch Exception e
          (log/warn e "unable to send tool deployment notification for" m))))))

(defn send-tool-request-notification
  "Sends notification of a successful tool request submission to the user."
  [tool-req user-details]
  (let [this-update (last (:history tool-req))
        comments    (:comments this-update)]
    (try
      (send-notification {:type           "tool_request"
                          :user           (:username user-details)
                          :subject        (str "Tool Request " (:name tool-req) " Submitted")
                          :email          true
                          :email_template "tool_request_submitted"
                          :payload        (assoc tool-req
                                            :email_address (:email user-details)
                                            :toolname      (:name tool-req)
                                            :comments      comments)})
      (catch Exception e
        (log/warn e "unable to send tool request submission notification for" tool-req)))))

(defn send-tool-request-update-notification
  "Sends notification of a tool request status change to the user."
  [tool-req user-details]
  (let [this-update (last (:history tool-req))
        status      (:status this-update)
        comments    (:comments this-update)
        subject     (str "Tool Request " (:name tool-req) " Status Changed to " status)]
    (try
      (send-notification {:type           "tool_request"
                          :user           (:username user-details)
                          :subject        subject
                          :email          true
                          :email_template (email-template-for status)
                          :payload        (assoc tool-req
                                            :email_address (:email user-details)
                                            :toolname      (:name tool-req)
                                            :comments      comments
                                            :status        status)})
      (catch Exception e
        (log/warn e "unable to send tool request update notification for" tool-req)))))

(defn send-job-status-update
  "Sends notification of an Agave or DE job status update to the user."
  [username email-address {job-name :name :as job-info}]
  (try
    (send-notification
     {:type           "analysis"
      :user           username
      :subject        (str job-name " status changed.")
      :message        (str job-name " " (string/lower-case (:status job-info)))
      :email          (if (#{"Completed" "Failed"} (:status job-info)) true false)
      :email_template "analysis_status_change"
      :payload        (assoc job-info
                        :analysisname          (:name job-info)
                        :analysisdescription   (:description job-info)
                        :analysisstatus        (:status job-info)
                        :analysisstartdate     (ut/format-timestamp (:startdate job-info))
                        :analysisresultsfolder (:resultfolderid job-info)
                        :email_address         email-address
                        :action                "job_status_change"
                        :user                  username)})
    (catch Exception e
      (log/warn e "unable to send job status update notification for" (:id job-info)))))
