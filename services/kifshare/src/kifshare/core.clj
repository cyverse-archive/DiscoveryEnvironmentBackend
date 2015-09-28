(ns kifshare.core
  (:gen-class)
  (:use compojure.core
        [ring.middleware
         params
         keyword-params
         nested-params
         multipart-params
         stacktrace])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as resp]
            [kifshare.config :as cfg]
            [kifshare.controllers :as controllers]
            [clojure.string :as string]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

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
  (GET "/favicon.ico" []
       (static-resp (cfg/favicon-path)))

  (GET "/resources/:rsc-name"
       [rsc-name]
       (static-resp rsc-name))

  (GET "/resources/css/:rsc-name"
       [rsc-name]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/css-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/js/:rsc-name"
       [rsc-name]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/js-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/flash/:rsc-name"
       [rsc-name]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/flash-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/resources/img/:rsc-name"
       [rsc-name]
       (let [resource-root (ft/path-join (cfg/resources-root) (cfg/img-dir))]
         (static-resp rsc-name :root resource-root)))

  (GET "/robots.txt" []
       (static-resp (cfg/robots-txt-path)))

  (HEAD "/d/:ticket-id/:filename" [ticket-id]
        (controllers/file-info ticket-id))

  (GET "/d/:ticket-id/:filename" [ticket-id filename :as request]
       (controllers/download-file ticket-id filename request))

  (HEAD "/d/:ticket-id" [ticket-id]
        (controllers/file-info ticket-id))

  (GET "/d/:ticket-id" [ticket-id :as request]
       (controllers/download-ticket ticket-id request))

  (GET "/:ticket-id" [ticket-id :as request]
       (controllers/get-ticket ticket-id request))

  (route/resources "/")

  (route/not-found "Not found!"))

(def svc-info
  {:desc "Service that serves up public files from iRODS."
   :app-name "kifshare"
   :group-id "org.iplantc"
   :art-id "kifshare"
   :service "kifshare"})

(defn wrap-log-request
  [handler]
  (fn [request]
    (log/info request)
    (handler request)))

(defn site-handler [routes]
  (-> routes
      wrap-log-request
      wrap-multipart-params
      wrap-keyword-params
      wrap-nested-params
      wrap-params
      wrap-stacktrace))

(def app
  (site-handler kifshare-routes))

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/kifshare.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn- override-buffer-size
  [opts]
  (or (:buffer-size opts)
      (* 1024 (Integer/parseInt (get @cfg/props "kifshare.app.download-buffer-size")))))

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The config file does not exist.")))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (cfg/local-init (:config options))
      (cfg/jargon-init)
      (cfg/log-config)
      (with-redefs [clojure.java.io/buffer-size override-buffer-size]
        (let [port (Integer/parseInt (string/trim (get @cfg/props "kifshare.app.port")))]
          (log/warn "Configured listen port is: " port)
          (jetty/run-jetty app {:port port}))))))
