(ns anon-files.core
  (:gen-class)
  (:require [compojure.route :as route]
            [clojure-commons.config :as cc]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]))

(def svc-info
  {:desc "A service that serves up files shared with the iRODS anonymous user."
   :app-name "anon-files"
   :group-id "org.iplantc"
   :art-id "anon-files"})

(defn -main
  [& args]
  (let [[options arguments errors summary] (ccli/handle-args svc-info args)]
    (println "hello")))
