(ns info-typer.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [clj-jargon.init :as init]
            [common-cli.core :as ccli]
            [info-typer.config :as cfg]
            [info-typer.messaging :as messages]
            [service-logging.thread-context :as tc]))


(def ^:private svc-info
  {:desc     "DE message handling service for file info type detection"
   :app-name "info-typer"
   :group-id "org.iplantc"
   :art-id   "info-typer"
   :service  "info-typer"})


(defn- cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/info-typer.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])


(defn- mk-jargon-cfg
  []
  (init/init (cfg/irods-host)
    (cfg/irods-port)
    (cfg/irods-user)
    (cfg/irods-pass)
    (cfg/irods-home)
    (cfg/irods-zone)
    (cfg/irods-resc)
    :max-retries (cfg/irods-max-retries)
    :retry-sleep (cfg/irods-retry-sleep)
    :use-trash   (cfg/irods-use-trash)))


(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 "The config file does not exist."))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (cfg/load-config-from-file (:config options))
      (messages/receive (mk-jargon-cfg)))))
