(ns iplant-email.core
  (:gen-class)
  (:use [compojure.core])
  (:require [clojure.tools.logging :as log]
            [iplant-email.send-mail :as sm]
            [iplant-email.json-body :as jb]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.cli :as cli]
            [iplant-email.config :as cfg]
            [clojure.string :as string]))

(defroutes email-routes
  (GET "/" [] "Welcome to iPlant Email!")

  (POST "/" {body :body} (sm/do-send-email body)))

(defn site-handler [routes]
  (-> routes jb/parse-json-body))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]
   ["-c" "--config PATH" "Path to the config file"]
   ["-h" "--help"]])

(defn usage
  [summary]
  (->> ["Sends email for the Discovery Environment"
        ""
        "Usage: iplant-email [options]"
        ""
        "Options:"
        summary]
       (string/join \newline)))

(defn error-msg
  [errors]
  (str "Errors:\n\n" (string/join \newline errors)))

(defn exit
  [status message]
  (println message)
  (System/exit status))

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

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (configurate options)
    (jetty/run-jetty (site-handler email-routes) {:port (port-number options)})))
