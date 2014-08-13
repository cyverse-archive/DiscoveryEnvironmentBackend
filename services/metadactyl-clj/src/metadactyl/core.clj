(ns metadactyl.core
  (:gen-class)
  (:use [clojure.java.io :only [file]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.core :only [GET PUT POST]]
        [metadactyl.beans]
        [metadactyl.kormadb]
        [metadactyl.routes.params]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [metadactyl.routes.apps :as app-routes]
            [metadactyl.routes.legacy :as legacy-routes]
            [metadactyl.util.config :as config]
            [ring.adapter.jetty :as jetty]))

(defn- init-service
  "Initializes the service."
  []
  (init-registered-beans)
  (define-database))

(defn- iplant-conf-dir-file
  [filename]
  (when-let [conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (let [f (file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))

(defn- cwd-file
  [filename]
  (let [f (file filename)]
    (when (.isFile f) (.getPath f))))

(defn- classpath-file
  [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.findResource filename)
      (.toURI)
      (file)))

(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))

(defn- find-config-file
  []
  (let [conf-file "metadactyl.properties"]
    (or (iplant-conf-dir-file conf-file)
        (cwd-file conf-file)
        (classpath-file conf-file)
        (no-configuration-found conf-file))))

(defn load-config-from-file
  "Loads the configuration settings from a properties file."
  ([]
     (load-config-from-file (find-config-file)))
  ([cfg-path]
     (config/load-config-from-file cfg-path)
     (init-service)))

(defapi app
  (middlewares
   [wrap-keyword-params
    wrap-query-params]
   (swagger-ui "/api")
   (swagger-docs "/api/api-docs"
                 :title "Metadactyl API"
                 :description "Documentation for the Metadactyl REST API"
                 :apiVersion "0.0.2")
   (swaggered "apps"
              :description "Discovery Environment App endpoints."
              (context "/apps" [:as {params :params}]
                       (store-current-user app-routes/apps params)))
   (swaggered "secured"
              :description "Secured Discovery Environment App endpoints."
              (context "/secured" [:as {params :params}]
                       (store-current-user legacy-routes/secured-routes params)))
   (swaggered "unsecured"
              :description "Unsecured Discovery Environment App endpoints."
              legacy-routes/metadactyl-routes)))

(def svc-info
  {:desc "Framework for hosting DiscoveryEnvironment metadata services."
   :app-name "metadactyl"
   :group-id "org.iplantc"
   :art-id "metadactyl"})

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/metadactyl.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
    (when-not (fs/exists? (:config options))
      (ccli/exit 1 (str "The config file does not exist.")))
    (when-not (fs/readable? (:config options))
      (ccli/exit 1 "The config file is not readable."))
    (load-config-from-file (:config options))
    (log/warn "Listening on" (config/listen-port))
    (jetty/run-jetty app {:port (config/listen-port)})))
