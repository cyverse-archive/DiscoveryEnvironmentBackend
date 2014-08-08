(ns monkey.core
  (:gen-class)
  (:use [slingshot.slingshot :only [throw+]])
  (:require [me.raynes.fs :as fs]
            [clojure-commons.config :as cfg]
            [common-cli.core :as cli]
            [monkey.actions :as actions]
            [monkey.index :as index]
            [monkey.props :as props]
            [monkey.tags :as tags]))


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


(defn- load-config-from-file
  [cfg-path]
  (let [p (ref nil)]
    (cfg/load-config-from-file cfg-path p)
    (when-not (props/validate @p)
      (throw+ "The configuration parameters are invalid."))
    @p))


(defn- reindex
  [props]
  (actions/sync-index (index/mk-index props) (tags/mk-tags props)))


(defn- listen
  [props])


(defn -main
  [& args]
  (let [{:keys [options _ _ _]} (cli/handle-args svc-info args (fn [] cli-options))]
    (when-not (fs/exists? (:config options))
      (cli/exit 1 "The config file does not exist."))
    (when-not (fs/readable? (:config options))
      (cli/exit 1 "The config file is not readable."))
    (let [props (load-config-from-file (:config options))]
      (if (:reindex options)
        (reindex props)
        (listen props)))))
