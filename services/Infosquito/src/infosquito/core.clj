(ns infosquito.core
  "This namespace defines the entry point for Infosquito. All state should be in here."
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :as ss]
            [clojure-commons.config :as config]
            [infosquito.actions :as actions]
            [infosquito.exceptions :as exn]
            [infosquito.icat :as icat]
            [infosquito.messages :as messages]
            [infosquito.props :as props]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc])
  (:import [java.util Properties]))


(defn- log-invalid-prop
  [prop-name]
  (log/error "configuration setting," prop-name ", is undefined"))


(defn- validate-props
  [p]
  (when-not (props/validate p log-invalid-prop)
    (ss/throw+ "The configuration parameters are invalid.")))


(defn- load-config-from-file
  [config-path]
  (let [p (ref nil)]
    (config/load-config-from-file config-path p)
    (validate-props @p)
    @p))


(defn- exit
  [msg]
  (log/fatal msg)
  (System/exit 1))


(defmacro ^:private trap-exceptions!
  [& body]
  `(ss/try+
     (do ~@body)
     (catch Object o# (log/error (exn/fmt-throw-context ~'&throw-context)))))


(defn cli-options
  []
  [["-c" "--config PATH" "sets the local configuration file to be read."
    :default "/etc/iplant/de/infosquito.properties"]
   ["-r" "--reindex" "reindex the iPlant Data Store and exit"]
   ["-v" "--version" "print the version and exit"]
   ["-h" "--help" "show help and exit"]])


(def svc-info
  {:desc "An ICAT database crawler used to index the contents of iRODS."
   :app-name "infosquito"
   :group-id "org.iplantc"
   :art-id "infosquito"
   :service "infosquito"})

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 "The config file does not exist."))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (let [props (load-config-from-file (:config options))]
        (if (:reindex options)
          (actions/reindex props)
          (messages/repeatedly-connect props))))))
