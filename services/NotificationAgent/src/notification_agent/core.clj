(ns notification-agent.core
  (:gen-class)
  (:use [clojure-commons.error-codes :only [trap]]
        [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.core]
        [ring.middleware keyword-params nested-params]
        [notification-agent.delete]
        [notification-agent.job-status]
        [notification-agent.notifications]
        [notification-agent.query]
        [notification-agent.seen]
        [slingshot.slingshot :only [try+]])
  (:require [compojure.route :as route]
            [clojure.tools.logging :as log]
            [notification-agent.config :as config]
            [notification-agent.db :as db]
            [ring.adapter.jetty :as jetty]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]))

(defn- job-status
  "Handles a job status update request."
  [body]
  (trap :job-status #(handle-job-status body)))

(defn- notification
  "Handles a generic notification request."
  [body]
  (trap :notification #(handle-notification-request body)))

(defn- delete
  "Handles a message deletion request."
  [params body]
  (trap :delete #(delete-messages params body)))

(defn- delete-all
  "Handles a request to delete all messages for a user."
  [params]
  (trap :delete-all #(delete-all-messages params)))

(defn- unseen-messages
  "Handles a query for unseen messages."
  [query]
  (trap :unseen-messages #(get-unseen-messages query)))

(defn- messages
  "Handles a request for a paginated message view."
  [query]
  (trap :messages #(get-paginated-messages query)))

(defn- count-msgs
  "Handles a request to count messages."
  [query]
  (trap :count-messages #(count-messages query)))

(defn- last-ten
  "Handles a request to get the most recent ten messages."
  [query]
  (trap :last-ten-messages #(last-ten-messages query)))

(defn- mark-seen
  "Handles a request to mark one or messages as seen."
  [body params]
  (trap :seen #(mark-messages-seen body params)))

(defn- mark-all-seen
  "Handles a request to mark all messages for a user as seen."
  [body]
  (trap :mark-all-seen #(mark-all-messages-seen body)))

(defn- admin-add-system-notification
  "Handles a request to add a system notification."
  [body]
  (trap :admin-add-system-notification #(handle-add-system-notif body)))

(defn- admin-list-system-notifications
  "Handles a request to list active system notifications."
  [params]
  (trap :admin-list-system-notifications #(handle-system-notification-listing params)))

(defn- admin-get-system-notification
  "Handles retrieving a system notification by uuid."
  [uuid]
  (trap :admin-get-system-notification #(handle-get-system-notif uuid)))

(defn- admin-update-system-notification
  "Handles updating a system notification."
  [uuid body]
  (trap :admin-update-system-notification #(handle-update-system-notif uuid body)))

(defn- admin-delete-system-notification
  "Handles deleting a system notification."
  [uuid]
  (trap :admin-delete-system-notification #(handle-delete-system-notif uuid)))

(defn- admin-get-system-notification-types
  "Handles getting a list of system notification types."
  []
  (trap :admin-get-system-notification-types #(handle-get-system-notif-types)))

(defn- user-system-messages
  [query]
  (trap :system-messages #(get-system-messages query)))

(defn- user-system-new-messages
  [query]
  (trap :system-new-messages #(get-new-system-messages query)))

(defn- user-system-unseen-messages
  [params]
  (trap :system-unseen-messages #(get-unseen-system-messages params)))

(defn- user-system-messages-received
  [body query]
  (trap :system-messages-received #(mark-system-messages-received body query)))

(defn- user-all-system-messages-received
  [body]
  (trap :all-system-messages-received #(mark-all-system-messages-received body)))

(defn- user-system-messages-seen
  [body query]
  (trap :system-messages-seen #(mark-system-messages-seen body query)))

(defn- user-all-system-messages-seen
  [body]
  (trap :all-system-messages-seen #(mark-all-system-messages-seen body)))

(defn- user-delete-system-messages
  [params body]
  (trap :delete-system-messages #(delete-system-messages params body)))

(defn- user-delete-all-system-messages
  [params]
  (trap :delete-all-system-messages #(delete-all-system-messages params)))

(defroutes notificationagent-routes
  (GET  "/" [] "Welcome to the notification agent!\n")
  (POST "/job-status" [:as {body :body}] (job-status body))
  (POST "/notification" [:as {body :body}] (notification body))
  (POST "/delete" [:as {:keys [params body]}] (delete params body))
  (DELETE "/delete-all" [:as {params :params}] (delete-all params))
  (POST "/seen" [:as {body :body params :params}] (mark-seen body params))
  (POST "/mark-all-seen" [:as {body :body}] (mark-all-seen body))
  (GET  "/unseen-messages" [:as {params :params}] (unseen-messages params))
  (GET  "/messages" [:as {params :params}] (messages params))
  (GET  "/count-messages" [:as {params :params}] (count-msgs params))
  (GET  "/last-ten-messages" [:as {params :params}] (last-ten params))

  ;;;DE UI facing APIs for system notifications
  (GET   "/system/messages" [:as {params :params}]
         (user-system-messages params))

  (GET "/system/new-messages" [:as {params :params}]
       (user-system-new-messages params))

  (GET "/system/unseen-messages" [:as {params :params}]
       (user-system-unseen-messages params))

  (POST "/system/received" [:as {body :body params :params}]
        (user-system-messages-received body params))

  (POST "/system/mark-all-received" [:as {body :body}]
        (user-all-system-messages-received body))

  (POST  "/system/seen" [:as {body :body params :params}]
         (user-system-messages-seen body params))

  (POST "/system/mark-all-seen" [:as {body :body}]
        (user-all-system-messages-seen body))

  (POST "/system/delete" [:as {:keys [params body]}]
        (user-delete-system-messages params body))

  (DELETE "/system/delete-all" [:as {params :params}]
          (user-delete-all-system-messages params))

  ;;;Admin-only facing APIs
  (PUT "/admin/system" [:as {body :body}]
       (admin-add-system-notification body))

  (GET "/admin/system" [:as {params :params}]
       (admin-list-system-notifications params))

  (GET "/admin/system/:uuid" [uuid :as {body :body}]
       (admin-get-system-notification uuid))

  (POST "/admin/system/:uuid" [uuid :as {body :body}]
        (admin-update-system-notification uuid body))

  (DELETE "/admin/system/:uuid" [uuid]
          (admin-delete-system-notification uuid))

  (GET "/admin/system-types" []
       (admin-get-system-notification-types))

  (route/not-found "Unrecognized service path.\n"))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-lcase-params
      wrap-nested-params
      wrap-query-params))

(def app
  (site-handler notificationagent-routes))

(defn- init-service
  []
  (db/define-database))

(defn load-config-from-file
  [cfg-path]
  (config/load-config-from-file cfg-path)
  (init-service))

(defn load-config-from-zookeeper
  []
  (config/load-config-from-zookeeper)
  (init-service))

(def svc-info
  {:desc "A web service for storing and forwarding notifications."
   :app-name "notificationagent"
   :group-id "org.iplantc"
   :art-id "notificationagent"})

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 "The config file does not exist."))
    (load-config-from-file (:config options))
    (future (initialize-job-status-service))
    (log/warn "Listening on" (config/listen-port))
    (jetty/run-jetty (site-handler notificationagent-routes) {:port (config/listen-port)})))

