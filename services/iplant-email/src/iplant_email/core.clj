(ns iplant-email.core
  (:gen-class)
  (:use compojure.core)
  (:require [cheshire.core :as cheshire]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.tools.logging :as log]
            [clojure-commons.config :as cfg]
            [iplant-email.send-mail :as sm]
            [iplant-email.json-body :as jb]
            [ring.adapter.jetty :as jetty]
            [iplant-email.json-validator :as jv]
            [iplant-email.templatize :as tmpl]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]))

(def config (ref nil))

(defn listen-port
  []
  (Integer/parseInt (get @config "iplant-email.app.listen-port")))

(defn smtp-host
  []
  (get @config "iplant-email.smtp.host"))

(defn smtp-from-addr
  []
  (get @config "iplant-email.smtp.from-address"))

(defn format-exception
  "Formats a raised exception as a JSON object. Returns a response map."
  [exception]
  (log/debug "format-exception")
  (let [string-writer (java.io.StringWriter.)
        print-writer  (java.io.PrintWriter. string-writer)]
    (. exception printStackTrace print-writer)
    (let [localized-message (. exception getLocalizedMessage)
          stack-trace       (. string-writer toString)]
      (log/warn (str localized-message stack-trace))
      {:status 500
       :body (cheshire/encode {:message     localized-message
                               :stack-trace stack-trace})})))

(defroutes email-routes
  (GET "/" [] "Welcome to iPlant Email!")

  (POST "/" {body :body}
        (log/debug (str "Received request with body: " (cheshire/encode body)))
        (cond
          (not (jv/valid? body {:template string?}))
          {:status 500 :body body}

          :else
          (try
            (let [template-name   (:template body)
                  template-values (:values body)
                  to-addr         (:to body)
                  cc-addr         (:cc body)
                  subject         (:subject body)
                  from-addr       (or (:from-addr body) (smtp-from-addr))
                  from-name       (:from-name body)
                  email-body      (tmpl/create-email template-name template-values)]
              (sm/send-email
                {:host (smtp-host)
                 :to-addr to-addr
                 :cc-addr cc-addr
                 :from-addr from-addr
                 :from-name from-name
                 :subject subject
                 :body email-body})
              (log/debug (str "Successfully sent email for request: " (cheshire/encode body)))
              {:status 200 :body "Email sent."})
            (catch Exception e
              (format-exception e))))))

(defn site-handler [routes]
  (-> routes
    jb/parse-json-body))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/iplant-email.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(def svc-info
  {:desc "Sends out emails for the DE."
   :app-name "iplant-email"
   :group-id "org.iplantc"
   :art-id "iplant-email"})

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The config file does not exist.")))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (cfg/load-config-from-file (:config options) config)
    (jetty/run-jetty (site-handler email-routes) {:port (listen-port)})))
