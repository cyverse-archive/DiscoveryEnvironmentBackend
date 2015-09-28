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
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

(defn cli-options
  []
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]

   ["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/tree-urls.properties"
    :validate [#(fs/exists? %) "Config file must exist."
               #(fs/readable? %) "Config file must be readable."]]

   ["-v" "--version" "Print out the version number."]

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
    (log/info (cfg/pprint-to-string request))
    (let [resp (handler request)]
      (log/info (cfg/pprint-to-string (dissoc resp :body)))
      resp)))

(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [formatted-exception (format-exception e)]
          (log/error (cfg/pprint-to-string request) "\n" formatted-exception)
          (-> (response formatted-exception) (status 500)))))))

(def svc-info
  {:desc "DE API for managing tree urls."
   :app-name "tree-urls"
   :group-id "org.iplantc"
   :art-id "tree-urls"
   :service "tree-urls"})

(def app
  (-> app-routes
      (wrap-logging)
      (wrap-json-body)
      (wrap-json-response)
      (wrap-exception)))

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The default --config file " (:config options) " does not exist.")))
      (cfg/load-config options)
      (connect-db)
      (log/info "Started listening on" (:port @cfg/cfg))
      (jetty/run-jetty app {:port (Integer/parseInt (:port @cfg/cfg))}))))
