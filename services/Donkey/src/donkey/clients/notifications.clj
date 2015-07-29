(ns donkey.clients.notifications
  (:use [clojure-commons.client :only [build-url-with-query]]
        [donkey.util.config :only [notificationagent-base]]
        [donkey.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [donkey.clients.notifications.raw :as raw]))

(defn notificationagent-url
  "Builds a URL that can be used to connect to the notification agent."
  ([relative-url]
     (notificationagent-url relative-url {}))
  ([relative-url query]
     (build-url-with-query (notificationagent-base)
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

(defn mark-all-notifications-seen
  []
  (raw/mark-all-notifications-seen (cheshire/encode (add-current-user-to-map {}))))
