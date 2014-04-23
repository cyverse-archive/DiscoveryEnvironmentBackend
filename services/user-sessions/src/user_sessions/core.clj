(ns user-sessions.core
  (:gen-class)
  (:use [compojure.core]
        [user-sessions.serve]
        [user-sessions.config]
        [ring.middleware.json :only [wrap-json-response wrap-json-body]])
  (:require [compojure.route :as route]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]
            [common-cfg.cfg :as cfg]
            [taoensso.timbre :as timbre]
            [me.raynes.fs :as fs])
  (:import [org.apache.log4j Logger]))

(timbre/refer-timbre)

(def log-levels (mapv name timbre/levels-ordered))

(defn cli-options
  []
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]

   ["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/anon-files.edn"
    :validate [#(fs/exists? %) "Config file must exist."
               #(fs/readable? %) "Config file must be readable."]]

   ["-v" "--version" "Print out the version number."]

   ["-f" "--log-file PATH" "Path to the log file."
    :validate [#(fs/exists? (fs/parent %1))
               "Directory containing the log file must exist."

               #(fs/readable? (fs/parent %1))
               "Directory containing the log file must be readable."]]

   ["-s" "--log-size SIZE" "Max Size of the logs in MB."
    :parse-fn #(* (Integer/parseInt %) 1024)]

   ["-b" "--log-backlog MAX" "Max number of rotated logs to retain."
    :parse-fn #(Integer/parseInt %)]

   ["-l" "--log-level LEVEL" (str "One of: " (string/join " " log-levels))
    :parse-fn #(keyword (string/lower-case %))
    :validate [#(contains? (set timbre/levels-ordered) %)
               (str "Log level must be one of: " (string/join " " log-levels))]]

   ["-h" "--help"]])

(defroutes session-routes
  (HEAD "/:username" [username :as req] (spy (head-req username req)))
  (GET "/:username" [username :as req] (spy (get-req username req)))
  (PUT "/:username" [username :as req] (spy (put-req username req)))
  (POST "/:username" [username :as req] (spy (post-req username req)))
  (DELETE "/:username" [username :as req] (spy (delete-req username))))

(def app
  (-> session-routes
      (wrap-json-body)
      (wrap-json-response)))

(def svc-info
  {:desc "DE API for managing user sessions."
   :app-name "user-sessions"
   :group-id "org.iplantc"
   :art-id "user-sessions"})

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The default --config file " (:config options) " does not exist.")))
    (cfg/load-config options)
    (connect-db)
    (info "Started listening on" (:port @cfg/cfg))
    (jetty/run-jetty app {:port (:port @cfg/cfg)})))

