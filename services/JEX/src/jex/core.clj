(ns jex.core
  (:gen-class)
  (:use [jex.config]
        [compojure.core]
        [ring.middleware
         params
         keyword-params
         nested-params
         multipart-params
         cookies
         session]
        [clojure-commons.error-codes]
        [clojure.java.classpath]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as rsp-utils]
            [ring.adapter.jetty :as jetty]
            [jex.process :as jp]
            [jex.json-body :as jb]
            [clojure.java.io :as ds]
            [cheshire.core :as cheshire]
            [me.raynes.fs :as fs]
            [common-cli.core :as ccli]
            [clojure.tools.logging :as log]
            [common-cfg.cfg :as cfg]
            [service-logging.thread-context :as tc]))

(defn do-submission
  "Handles a request on /. "
  [request]
  (try
    (let [body (:body request)]
      (log/warn "Received job request:")
      (log/warn (cheshire/encode body))

      (if (jp/validate-submission body)
        (let [[exit-code dag-id] (jp/submit body)]
          (cond
            (not= exit-code 0)
            (throw+ {:error_code "ERR_FAILED_NON_ZERO"})

            :else
            {:sub_id dag-id}))
        (throw+ {:error_code "ERR_INVALID_JSON"})))
    (catch Exception e
      (log/error e "job submission failed")
      (throw+ {:error_code "ERR_UNHANDLED_EXCEPTION"}))))

(defroutes jex-routes
  (GET "/" [] "Welcome to the JEX.")

  (POST "/" request
        (trap "submit" do-submission request))

  (POST "/arg-preview" request
        (trap "arg-preview" jp/cmdline-preview (:body request)))

  (DELETE "/stop/:uuid" [uuid]
          (trap "stop" jp/stop-analysis uuid)))

(defn req-logger
  [handler]
  (fn [req]
    (log/info "request received:" req)
    (handler req)))

(def svc-info
  {:desc "Submits jobs to a Condor cluster for the DE."
   :app-name "jex"
   :group-id "org.iplantc"
   :art-id "jex"
   :service "jex"})

(defn site-handler [routes]
  (-> routes
      req-logger
      jb/parse-json-body
      wrap-errors))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/jex.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The config file does not exist.")))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (log/warn (:config options))
      (cfg/load-config options)
      (jetty/run-jetty (site-handler jex-routes) {:port (listen-port)}))))
