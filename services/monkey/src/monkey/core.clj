(ns monkey.core
  (:gen-class)
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [clojure-commons.config :as cfg]
            [common-cli.core :as cli]
            [monkey.actions :as actions]
            [monkey.props :as props]))


(def ^{:private true :const true} svc-info
  {:desc "🐒 -- a metadata database crawler"
   :app-name "monkey"
   :group-id "org.iplantc"
   :art-id "monkey"})


(def ^{:private true :const true} cli-options
  [["-c" "--config PATH" "sets the local configuration file to be read."
      :default "/etc/iplant/de/monkey.properties"]
   ["-r" "--reindex" "reindex the metadata database and exit"]
   ["-h" "--help" "show help and exit"]])


;; TODO implement
(defn- load-config-from-file
  [cfg-path]
  {})


(defn -main
  [& args]
  (let [{:keys [options _ _ _]} (cli/handle-args svc-info args (fn [] cli-options))]
    (when-not (fs/exists? (:config options))
      (cli/exit 1 "The config file does not exist."))
    (when-not (fs/readable? (:config options))
      (cli/exit 1 "The config file is not readable."))
    (let [props (load-config-from-file (:config options))]
      (if (:reindex options)
        (actions/reindex props)
        (actions/listen props)))))