(ns iplant-email.core
  (:gen-class)
  (:use [compojure.core])
  (:require [clojure.tools.logging :as log]
            [iplant-email.send-mail :as sm]
            [iplant-email.json-body :as jb]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.cli :as cli]
            [iplant-email.config :as cfg]))

(defroutes email-routes
  (GET "/" [] "Welcome to iPlant Email!")

  (POST "/" {body :body} (sm/do-send-email body)))

(defn site-handler [routes]
  (-> routes jb/parse-json-body))

(defn -main
  [& args]
  (cfg/load-config-from-zookeeper)
  (jetty/run-jetty (site-handler email-routes) {:port (cfg/listen-port)}))
