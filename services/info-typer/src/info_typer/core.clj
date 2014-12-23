(ns info-typer.core
  (:gen-class)
  (:require [me.raynes.fs :as fs]
            [common-cli.core :as ccli]
            [info-typer.config :as config]
            [info-typer.messaging :as messages]))


(def ^:private svc-info
  {:desc     "DE message handling service for file info type detection"
   :app-name "info-typer"
   :group-id "org.iplantc"
   :art-id   "info-typer"})


(defn- cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/info-typer.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])


(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 "The config file does not exist."))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (config/load-config-from-file (:config options))
    (messages/receive)))
