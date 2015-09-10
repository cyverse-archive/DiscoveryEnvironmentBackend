(ns metadata.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [metadata.util.db :as db]
            [metadata.util.config :as config]
            [ring.adapter.jetty :as jetty]
            [service-logging.thread-context :as tc]))

(defn init-service
  ([]
    (init-service config/default-config-file))
  ([cfg-path]
    (config/load-config-from-file cfg-path)
    (db/define-database)))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default config/default-config-file
    :validate [#(fs/exists? %) "The config file does not exist."
               #(fs/readable? %) "The config file is not readable."]]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/with-logging-context config/svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args config/svc-info args cli-options)]
      (init-service (:config options))
      (log/warn "Started listening on" (config/listen-port))
      (require 'metadata.routes) ; After init-service so the database is initialized
      (jetty/run-jetty (eval 'metadata.routes/app) {:port (config/listen-port)}))))
