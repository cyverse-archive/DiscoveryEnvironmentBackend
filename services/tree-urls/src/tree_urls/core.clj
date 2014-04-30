(ns tree-urls.core
  (:gen-class)
  (:use [compojure.core]
        [tree-urls.serve]
        [tree-urls.config]
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [io.aviso.exception :only [format-exception]]
        [ring.util.response :only [response status]])
  (:require [compojure.route :as route]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [ring.adapter.jetty :as jetty]
            [common-cfg.cfg :as cfg]
            [taoensso.timbre :as timbre]
            [me.raynes.fs :as fs]))

(timbre/refer-timbre)

(def log-levels (mapv name timbre/levels-ordered))

(defn cli-options
  []
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]

   ["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/tree-urls.edn"
    :validate [#(fs/exists? %) "Config file must exist."
               #(fs/readable? %) "Config file must be readable."]]

   ["-v" "--version" "Print out the version number."]

   ["-f" "--log-file PATH" "Path to the log file."
    :validate [#(fs/exists? (fs/parent %1))
               "Directory containing the log file must exist."

               #(fs/readable? (fs/parent %1))
               "Directory containing the log file must be readable."]]

   ["-s" "--log-size SIZE" "Max Size of the logs in MB."
    :parse-fn #(* (Integer/parseInt %) 1024)]

   ["-b" "--log-backlog MAX" "Max number of rotated logs to retain."
    :parse-fn #(Integer/parseInt %)]

   ["-l" "--log-level LEVEL" (str "One of: " (string/join " " log-levels))
    :parse-fn #(keyword (string/lower-case %))
    :validate [#(contains? (set timbre/levels-ordered) %)
               (str "Log level must be one of: " (string/join " " log-levels))]]

   ["-h" "--help"]])

(defroutes app-routes
  (GET "/" [] "Hello from tree-urls.")

  (GET "/:sha1" [sha1 :as req]
       (get-req sha1 req))

  (PUT "/:sha1" [sha1 :as req]
       (put-req sha1 req))

  (POST "/:sha1" [sha1 :as req]
        (post-req sha1 req))

  (DELETE "/:sha1" [sha1 :as req]
          (delete-req sha1 req)))

(defn wrap-logging [handler]
  (fn [request]
    (info (cfg/pprint-to-string request))
    (let [resp (handler request)]
      (info (cfg/pprint-to-string (dissoc resp :body)))
      resp)))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [formatted-exception (format-exception e)]
          (error (cfg/pprint-to-string request) "\n" formatted-exception)
          (-> (response formatted-exception) (status 500)))))))

(def app
  (-> app-routes
      (wrap-logging)
      (wrap-json-body)
      (wrap-json-response)
      (wrap-exception)))

(def svc-info
  {:desc "DE API for managing tree urls."
   :app-name "tree-urls"
   :group-id "org.iplantc"
   :art-id "tree-urls"})

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The default --config file " (:config options) " does not exist.")))
    (cfg/load-config options)
    (connect-db)
    (info "Started listening on" (:port @cfg/cfg))
    (jetty/run-jetty app {:port (:port @cfg/cfg)})))
