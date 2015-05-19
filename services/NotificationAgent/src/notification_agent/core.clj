(ns notification-agent.core
  (:gen-class)
  (:use [clojure.java.io :only [file]]
        [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [clojure-commons.middleware :only [wrap-log-requests]]
        [compojure.core]
        [korma.db :only [transaction]]
        [ring.middleware keyword-params nested-params]
        [notification-agent.delete]
        [notification-agent.notifications]
        [notification-agent.query]
        [notification-agent.seen]
        [slingshot.slingshot :only [try+]])
  (:require [compojure.route :as route]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [notification-agent.config :as config]
            [notification-agent.db :as db]
            [ring.adapter.jetty :as jetty]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

(defn- trap
  [ctx f]
  (transaction (ce/trap ctx f)))

(defroutes notificationagent-routes
  (GET  "/" []
        "Welcome to the notification agent!\n")

  (POST "/notification" [:as {body :body}]
        (trap :notification #(handle-notification-request body)))

  (POST "/delete" [:as {:keys [params body]}]
        (trap :delete #(delete-messages params body)))

  (DELETE "/delete-all" [:as {params :params}]
          (trap :delete-all #(delete-all-messages params)))

  (POST "/seen" [:as {body :body params :params}]
        (trap :seen #(mark-messages-seen body params)))

  (POST "/mark-all-seen" [:as {body :body}]
        (trap :mark-all-seen #(mark-all-messages-seen body)))

  (GET  "/unseen-messages" [:as {params :params}]
        (trap :unseen-messages #(get-unseen-messages params)))

  (GET  "/messages" [:as {params :params}]
        (trap :messages #(get-paginated-messages params)))

  (GET  "/count-messages" [:as {params :params}]
        (trap :count-messages #(count-messages params)))

  (GET  "/last-ten-messages" [:as {params :params}]
        (trap :last-ten-messages #(last-ten-messages params)))

  ;;;DE UI facing APIs for system notifications
  (GET   "/system/messages" [:as {params :params}]
         (trap :system-messages #(get-system-messages params)))

  (GET "/system/new-messages" [:as {params :params}]
       (trap :system-new-messages #(get-new-system-messages params)))

  (GET "/system/unseen-messages" [:as {params :params}]
       (trap :system-unseen-messages #(get-unseen-system-messages params)))

  (POST "/system/received" [:as {body :body params :params}]
        (trap :system-messages-received #(mark-system-messages-received body params)))

  (POST "/system/mark-all-received" [:as {body :body}]
        (trap :all-system-messages-received #(mark-all-system-messages-received body)))

  (POST  "/system/seen" [:as {body :body params :params}]
         (trap :system-messages-seen #(mark-system-messages-seen body params)))

  (POST "/system/mark-all-seen" [:as {body :body}]
        (trap :all-system-messages-seen #(mark-all-system-messages-seen body)))

  (POST "/system/delete" [:as {:keys [params body]}]
        (trap :delete-system-messages #(delete-system-messages params body)))

  (DELETE "/system/delete-all" [:as {params :params}]
          (trap :delete-all-system-messages #(delete-all-system-messages params)))

  ;;;Admin-only facing APIs
  (PUT "/admin/system" [:as {body :body}]
       (trap :admin-add-system-notification #(handle-add-system-notif body)))

  (GET "/admin/system" [:as {params :params}]
       (trap :admin-list-system-notifications #(handle-system-notification-listing params)))

  (GET "/admin/system/:uuid" [uuid]
       (trap :admin-get-system-notification #(handle-get-system-notif uuid)))

  (POST "/admin/system/:uuid" [uuid :as {body :body}]
        (trap :admin-update-system-notification #(handle-update-system-notif uuid body)))

  (DELETE "/admin/system/:uuid" [uuid]
          (trap :admin-delete-system-notification #(handle-delete-system-notif uuid)))

  (GET "/admin/system-types" []
       (trap :admin-get-system-notification-types #(handle-get-system-notif-types)))

  (route/not-found "Unrecognized service path.\n"))

(defn- init-service
  []
  (db/define-database))

(defn- iplant-conf-dir-file
  [filename]
  (when-let [conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (let [f (file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))

(defn- cwd-file
  [filename]
  (let [f (file filename)]
    (when (.isFile f) (.getPath f))))

(defn- classpath-file
  [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.findResource filename)
      (.toURI)
      (file)))

(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))

(defn- find-configuration-file
  []
  (let [conf-file "notificationagent.properties"]
    (or (iplant-conf-dir-file conf-file)
        (cwd-file conf-file)
        (classpath-file conf-file)
        (no-configuration-found conf-file))))

(defn load-config-from-file
  ([]
     (load-config-from-file (find-configuration-file)))
  ([cfg-path]
     (config/load-config-from-file cfg-path)
     (init-service)))

(def svc-info
  {:desc "A web service for storing and forwarding notifications."
   :app-name "notificationagent"
   :group-id "org.iplantc"
   :art-id "notificationagent"
   :service "notificationagent"})

(defn site-handler [routes]
  (-> routes
      wrap-log-requests
      tc/add-user-to-context
      wrap-keyword-params
      wrap-lcase-params
      wrap-nested-params
      wrap-query-params
      (tc/wrap-thread-context svc-info)))

(def app
  (site-handler notificationagent-routes))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/notificationagent.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/set-context! svc-info)
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 "The config file does not exist."))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (load-config-from-file (:config options))
    (log/warn "Listening on" (config/listen-port))
    (jetty/run-jetty (site-handler notificationagent-routes) {:port (config/listen-port)})))
