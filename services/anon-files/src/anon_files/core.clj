(ns anon-files.core
  (:gen-class)
  (:use [compojure.core]
        [anon-files.serve]
        [anon-files.config])
  (:require [compojure.route :as route]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]
            [common-cfg.cfg :as cfg]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

(defn cli-options
  []
  [["-p" "--port PORT" "Listen port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]

   ["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/anon-files.properties"
    :validate [#(fs/exists? %) "Config file must exist."
               #(fs/readable? %) "Config file must be readable."]]

   ["-v" "--version" "Print out the version number."]

   ["-h" "--help"]])

(defroutes app
  (HEAD "/*" [:as req] (log/spy (handle-head-request req)))
  (GET "/*" [:as req] (log/spy (handle-request req)))
  (OPTIONS "/*" [:as req] (log/spy (handle-options-request req))))

(def svc-info
  {:desc "A service that serves up files shared with the iRODS anonymous user."
   :app-name "anon-files"
   :group-id "org.iplantc"
   :art-id "anon-files"
   :service "anon-files"})

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The default --config file " (:config options) " does not exist.")))
      (cfg/load-config options)
      (log/info "Started listening on" (:port @cfg/cfg))
      (jetty/run-jetty app {:port (Integer/parseInt (:port @cfg/cfg))}))))
