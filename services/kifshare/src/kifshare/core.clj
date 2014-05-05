(ns kifshare.core
  (:gen-class)
  (:use compojure.core
        kifshare.config
        [ring.middleware
         params
         keyword-params
         nested-params
         multipart-params
         cookies
         session
         stacktrace]
        [clojure.core.memoize]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure-commons.clavin-client :as cl]
            [clojure-commons.props :as prps]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [kifshare.config :as cfg]
            [kifshare.controllers :as controllers]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs])
  (:use [clojure-commons.error-codes]))

(defn keep-alive
  [resp]
  (assoc-in resp [:headers "Connection"] "Keep-Alive"))

(defn caching
  [resp]
  (assoc-in resp [:headers "Cache-Control"]
            (str (cfg/client-cache-scope) ", max-age=" (cfg/client-cache-max-age))))

(defn static-resp
  [file-path & {:keys [root] :or {root (cfg/resources-root)}}]
  (-> (resp/file-response file-path {:root root})
      caching
      keep-alive))

(defroutes kifshare-routes
  (GET "/favicon.ico"
       req
       (static-resp (cfg/favicon-path)))

  (GET "/resources/:rsc-name"
       [rsc-name :as req]
       (static-resp rsc-name))

  (GET "/resources/css/:rsc-name"
       [rsc-name :as req]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/css-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/js/:rsc-name"
       [rsc-name :as req]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/js-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/flash/:rsc-name"
       [rsc-name :as req]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/flash-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/img/:rsc-name"
       [rsc-name :as req]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/img-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/robots.txt"
       req
       (static-resp (cfg/robots-txt-path)))

  (HEAD "/d/:ticket-id/:filename" [ticket-id filename as request]
        (controllers/file-info ticket-id filename request))

  (GET "/d/:ticket-id/:filename" [ticket-id filename :as request]
       (controllers/download-file ticket-id filename request))

  (HEAD "/d/:ticket-id" [ticket-id :as request]
        (controllers/file-info ticket-id request))

  (GET "/d/:ticket-id" [ticket-id :as request]
       (controllers/download-ticket ticket-id request))

  (GET "/:ticket-id" [ticket-id :as request]
       (controllers/get-ticket ticket-id request))

  (route/resources "/")

  (route/not-found "Not found!"))

(defn site-handler [routes]
  (-> routes
      wrap-multipart-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-stacktrace))

(defn parse-args
  [args]
  (log/debug "entered kifshare.core/parse-args")

  (cli/cli
   args
    ["-c" "--config"
     "Set the local config file to read from. Bypasses Zookeeper"
     :default nil]
    ["-h" "--help"
     "Show help."
     :default false
     :flag true]))

(def app
  (site-handler kifshare-routes))

(def svc-info
  {:desc "Service that serves up public files from iRODS."
   :app-name "kifshare"
   :group-id "org.iplantc"
   :art-id "kifshare"})

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn- override-buffer-size
  [opts]
  (or (:buffer-size opts)
      (* 1024 (Integer/parseInt (get @cfg/props "kifshare.app.download-buffer-size")))))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The config file does not exist.")))
    (cfg/local-init (:config options))
    (cfg/jargon-init)
    (with-redefs [clojure.java.io/buffer-size override-buffer-size]
      (let [port (Integer/parseInt (string/trim (get @cfg/props "kifshare.app.port")))]
        (log/warn "Configured listen port is: " port)
        (jetty/run-jetty app {:port port})))))

