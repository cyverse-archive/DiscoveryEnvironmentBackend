(ns iplant-email.core
  (:gen-class)
  (:use [compojure.core])
  (:require [clojure.tools.logging :as log]
            [iplant-email.send-mail :as sm]
            [iplant-email.json-body :as jb]
            [ring.adapter.jetty :as jetty]
            [iplant-email.config :as cfg]
            [clojure.string :as string]
            [common-cli.core :as ccli]))

(defroutes email-routes
  (GET "/" [] "Welcome to iPlant Email!")

  (POST "/" {body :body} (sm/do-send-email body)))

(defn site-handler [routes]
  (-> routes jb/parse-json-body))

(defn configurate
  [options]
  (if (:config options)
    (cfg/load-config-from-file (:config options))
    (cfg/load-config-from-zookeeper)))

(def port-number
  (memoize
   (fn [options]
     (if (:port options)
       (:port options)
       (cfg/listen-port)))))

(def svc-info
  {:desc "Sends email for the Discovery Environment"
   :app-name "iplant-email"
   :group-id "org.iplantc"
   :art-id "iplant-email"})

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args)]
    (configurate options)
    (jetty/run-jetty (site-handler email-routes) {:port (port-number options)})))
