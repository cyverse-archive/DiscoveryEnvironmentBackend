(ns user-sessions.core
  (:gen-class)
  (:use [compojure.core]
        [user-sessions.serve]
        [user-sessions.config]
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [io.aviso.exception :only [format-exception]]
        [ring.util.response :only [response status]])
  (:require [compojure.route :as route]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]
            [common-cfg.cfg :as cfg]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

(defn cli-options
  []
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]

   ["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/user-sessions.properties"
    :validate [#(fs/exists? %) "Config file must exist."
               #(fs/readable? %) "Config file must be readable."]]

   ["-v" "--version" "Print out the version number."]

   ["-h" "--help"]])

(defroutes session-routes
  (GET "/" [] "Hello from user-sessions.")

  (GET "/:username" [username :as req]
       (get-req username req))

  (PUT "/:username" [username :as req]
       (put-req username req))

  (POST "/:username" [username :as req]
        (post-req username req))

  (DELETE "/:username" [username :as req]
          (delete-req username req)))

(defn wrap-logging [handler]
  (fn [request]
    (log/info (cfg/pprint-to-string request))
    (let [resp (handler request)]
      (log/info (cfg/pprint-to-string (dissoc resp :body)))
      resp)))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [formatted-exception (format-exception e)]
          (log/error (cfg/pprint-to-string request) "\n" formatted-exception)
          (-> (response formatted-exception) (status 500)))))))

(defn wrap-user
  [handler]
  (fn [request]
    (let [username (last (string/split (:uri request) #"\/"))]
      (if-not (or (nil? username) (string/blank? username))
        (tc/set-context! {:user username}))
      (handler request))))

(def svc-info
  {:desc "DE API for managing user sessions."
   :app-name "user-sessions"
   :group-id "org.iplantc"
   :art-id "user-sessions"
   :service "user-sessions"})

(def app
  (-> session-routes
      (wrap-logging)
      (wrap-json-body)
      (wrap-json-response)
      (wrap-exception)
      (wrap-user)))

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The default --config file " (:config options) " does not exist.")))
      (cfg/load-config options)
      (connect-db)
      (log/info "Started listening on" (:port @cfg/cfg))
      (jetty/run-jetty app {:port (Integer/parseInt (:port @cfg/cfg))}))))
