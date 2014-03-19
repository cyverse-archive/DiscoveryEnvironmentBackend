(ns dewey.core
  (:gen-class)
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.rest :as es]
            [clj-jargon.init :as irods]
            [clojure-commons.config :as config]
            [dewey.amq :as amq]
            [dewey.curation :as curation]
            [dewey.status :as status])
  (:import [java.net URL]
           [java.util Properties]))


(defn- init-es
  [props]
  (let [url    (URL. "http" (get props "dewey.es.host") (Integer. (get props "dewey.es.port")) "")
        found? (try
                 (es/connect! (str url))
                 (log/info "Found elasticsearch")
                 true
                 (catch Exception e
                   (log/debug e)
                   (log/info "Failed to find elasticsearch. Retrying...")
                   false))]
    (when-not found?
      (Thread/sleep 1000)
      (recur props))))


(defn- init-irods
  [props]
  (irods/init (get props "dewey.irods.host")
              (get props "dewey.irods.port")
              (get props "dewey.irods.user")
              (get props "dewey.irods.password")
              (get props "dewey.irods.home")
              (get props "dewey.irods.zone")
              (get props "dewey.irods.default-resource")))


(defn- listen
  [props irods-cfg]
  (let [attached? (try
                    (amq/attach-to-exchange (get props "dewey.amqp.host")
                                            (Integer. (get props "dewey.amqp.port"))
                                            (get props "dewey.amqp.user")
                                            (get props "dewey.amqp.password")
                                            (get props "dewey.amqp.exchange.name")
                                            (Boolean. (get props "dewey.amqp.exchange.durable"))
                                            (Boolean. (get props "dewey.amqp.exchange.auto-delete"))
                                            (partial curation/consume-msg irods-cfg)
                                            "data-object.#"
                                            "collection.#")
                    (log/info "Attached to the AMQP broker.")
                    true
                    (catch Exception e
                      (log/debug (str e))
                      (log/info "Failed to attach to the AMQP broker. Retrying...")
                      false))]
    (when-not attached?
      (Thread/sleep 1000)
      (recur props irods-cfg))))


(defn- listen-for-status
  [props]
  (.start
   (Thread.
     (partial status/start-jetty (Integer/parseInt (get props "dewey.status.listen-port"))))))


(defn- update-props
  [load-props props]
  (let [props-ref (ref props)]
    (try+
      (load-props props-ref)
      (catch Object _
        (log/error "Failed to load configuration parameters.")))
    (when (.isEmpty @props-ref)
      (throw+ {:type :cfg-problem :msg "Don't have any configuration parameters."}))
    (when-not (= props @props-ref)
      (config/log-config props-ref))
    @props-ref))


(defn- run
  [props-loader]
  (let [props (update-props props-loader (Properties.))]
   (init-es props)
   (listen-for-status props)
   (listen props (init-irods props))))


(defn- parse-args
  [args]
  (cli/cli args
    ["-c" "--config" "sets the local configuration file to be read, bypassing Zookeeper"]
    ["-h" "--help"   "show help and exit" :flag true]))


(defn -main
  [& args]
  (try+
    (let [[opts _ help-str] (parse-args args)]
      (if (:help opts)
        (println help-str)
        (run (if-let [cfg-file (:config opts)]
               (partial config/load-config-from-file nil cfg-file)
               #(config/load-config-from-zookeeper % "dewey")))))
    (catch Object _
      (log/error (:throwable &throw-context) "UNEXPECTED ERROR - EXITING"))))
