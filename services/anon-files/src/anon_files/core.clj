(ns anon-files.core
  (:gen-class)
  (:use [compojure.core]
        [anon-files.serve])
  (:require [compojure.route :as route]
            [clojure-commons.config :as cc]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]
            [anon-files.config :as cfg]))

(defroutes app
  (GET "/*" [:as req] (handle-request req)))

(def svc-info
  {:desc "A service that serves up files shared with the iRODS anonymous user."
   :app-name "anon-files"
   :group-id "org.iplantc"
   :art-id "anon-files"})

(def port-number
  (memoize
   (fn [options]
     (if (:port options)
       (:port options)
       (cfg/listen-port)))))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args)]
    (when-not (:config options)
      (ccli/exit 1 "The --config option is required."))
    (cfg/load-config-from-file (:config options))
    (jetty/run-jetty app {:port (port-number options)})))
