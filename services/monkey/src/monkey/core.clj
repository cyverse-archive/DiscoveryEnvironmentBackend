(ns monkey.core
  (:gen-class)
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [clojure-commons.config :as cfg]
            [common-cli.core :as cli]
            [monkey.actions :as actions]
            [monkey.index :as index]
            [monkey.messenger :as msg]
            [monkey.props :as props]
            [monkey.tags :as tags]
            [service-logging.thread-context :as tc]))


(def ^{:private true :const true} svc-info
  {:desc "🐒 -- a metadata database crawler"
   :app-name "monkey"
   :group-id "org.iplantc"
   :art-id "monkey"
   :service "monkey"})


(def ^{:private true :const true} cli-options
  [["-c" "--config PATH" "sets the local configuration file to be read."
      :default "/etc/iplant/de/monkey.properties"]
   ["-r" "--reindex" "reindex the metadata database and exit"]
   ["-v" "--version" "print the version and exit"]
   ["-h" "--help" "show help and exit"]])


(defn- load-config-from-file
  [cfg-path]
  (let [p (ref nil)]
    (cfg/load-config-from-file cfg-path p)
    (cfg/log-config p)
    (when-not (props/validate @p)
      (throw+ "The configuration parameters are invalid."))
    @p))


(defn- reindex
  [props]
  (tags/with-tags props
                  #(actions/sync-index (actions/mk-monkey props (index/mk-index props) %))))


(defn- listen
  [props]
  (msg/listen props #(reindex props)))


(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (try+
      (let [{:keys [options _ _ _]} (cli/handle-args svc-info args (fn [] cli-options))]
        (when-not (fs/exists? (:config options))
          (cli/exit 1 "The config file does not exist."))
        (when-not (fs/readable? (:config options))
          (cli/exit 1 "The config file is not readable."))
        (let [props (load-config-from-file (:config options))]
          (if (:reindex options)
            (reindex props)
            (listen props))))
      (catch Object _
        (log/fatal (:message &throw-context)
                   (apply str (map #(str "\n\t" %) (:stack-trace &throw-context))))
        (log/fatal "EXITING")))))
