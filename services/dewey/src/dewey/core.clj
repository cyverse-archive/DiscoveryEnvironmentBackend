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
            [dewey.status :as status]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc])
  (:import [java.net URL]
           [java.util Properties]))


(defn- init-es
  "Establishes a connection to elasticsearch"
  [props]
  (let [url  (URL. "http" (get props "dewey.es.host") (Integer. (get props "dewey.es.port")) "")
        conn (try
               (es/connect(str url))
               (catch Exception e
                 (log/debug e)
                 nil))]
    (if conn
      (do
        (log/info "Found elasticsearch")
        conn)
      (do
        (log/info "Failed to find elasticsearch. Retrying...")
        (Thread/sleep 1000)
        (recur props)))))


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
  [props irods-cfg es]
  (let [attached? (try
                    (amq/attach-to-exchange (get props "dewey.amqp.host")
                                            (Integer. (get props "dewey.amqp.port"))
                                            (get props "dewey.amqp.user")
                                            (get props "dewey.amqp.password")
                                            (get props "dewey.amqp.exchange.name")
                                            (Boolean. (get props "dewey.amqp.exchange.durable"))
                                            (Boolean. (get props "dewey.amqp.exchange.auto-delete"))
                                            (partial curation/consume-msg irods-cfg es)
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
      (recur props irods-cfg es))))


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
    (listen-for-status props)
    (listen props (init-irods props) (init-es props))))


(def svc-info
  {:desc "Service that keeps an elasticsearch index synchronized with an iRODS repository."
   :app-name "dewey"
   :group-id "org.iplantc"
   :art-id "dewey"
   :service "dewey"})


(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/dewey.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])


(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (try+
     (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
       (when-not (fs/exists? (:config options))
         (ccli/exit 1 (str "The config file does not exist.")))
       (run (partial config/load-config-from-file (:config options))))
      (catch Object _
        (log/error (:throwable &throw-context) "UNEXPECTED ERROR - EXITING")))))
