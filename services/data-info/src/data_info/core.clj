(ns data-info.core
  (:gen-class)
  (:use [clojure-commons.lcase-params :only [wrap-lcase-params]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.api.legacy]
        [data-info.util.service]
        [ring.util.response :only [redirect]])
  (:require [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [data-info.util.config :as config]
            [clojure.tools.nrepl.server :as nrepl]
            [liberator.dev :as liberator]
            [me.raynes.fs :as fs]
            [ring.middleware.keyword-params :as params]
            [common-cli.core :as ccli]
            [service-logging.thread-context :as tc]
            [data-info.routes :as routes]
            [data-info.routes.data :as data-routes]
            [data-info.routes.exists :as exists-routes]
            [data-info.routes.home :as home-routes]
            [data-info.routes.navigation :as navigation-routes]
            [data-info.routes.stats :as stat-routes]
            [data-info.services.icat :as icat]
            [data-info.util :as util]))


(defn- start-nrepl
  []
  (nrepl/start-server :port 7888))


(defn- iplant-conf-dir-file
  [filename]
  (when-let [conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (let [f (io/file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))


(defn- cwd-file
  [filename]
  (let [f (io/file filename)]
    (when (.isFile f) (.getPath f))))


(defn- classpath-file
  [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.findResource filename)
      (.toURI)
      io/file))


(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))


(defn- find-configuration-file
  []
  (let [conf-file "data-info.properties"]
    (or (iplant-conf-dir-file conf-file)
        (cwd-file conf-file)
        (classpath-file conf-file)
        (no-configuration-found conf-file))))


(defn- load-configuration-from-file
  "Loads the configuration properties from a file."
  ([]
   (load-configuration-from-file (find-configuration-file)))

  ([path]
    (config/load-config-from-file path)))


(defn lein-ring-init
  []
  (load-configuration-from-file)
  (icat/configure-icat)
  (start-nrepl))


(defn repl-init
  []
  (load-configuration-from-file)
  (icat/configure-icat))

(defn context-middleware
  [handler]
  (tc/wrap-thread-context handler config/svc-info))

(defapi app
  (swagger-ui "/api")
  (swagger-docs
    {:info {:title "Discovery Environment Data Info API"
            :description "Documentation for the Discovery Environment Data Info REST API"
            :version "2.0.0"}})
  (GET "/" [] (redirect "/api"))
  (GET "/favicon.ico" [] {:status 404})
  (middlewares
    [tc/add-user-to-context
     wrap-query-params
     wrap-lcase-params
     params/wrap-keyword-params
     util/req-logger
     context-middleware]
    data-routes/data-operations
    exists-routes/existence-marker
    home-routes/home
    navigation-routes/navigation
    stat-routes/stat-gatherer)
  (middlewares
    [tc/add-user-to-context
     wrap-query-params
     wrap-lcase-params
     params/wrap-keyword-params
     util/req-logger
     #_(liberator/wrap-trace :header :ui)
     util/trap-handler
     context-middleware]
    routes/all-routes))


(defn- cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/data-info.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/set-context! config/svc-info)
  (let [{:keys [options arguments errors summary]} (ccli/handle-args config/svc-info
                                                                     args
                                                                     cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The config file does not exist.")))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (load-configuration-from-file (:config options))
    (icat/configure-icat)
    (jetty/run-jetty app {:port (config/listen-port)})))
