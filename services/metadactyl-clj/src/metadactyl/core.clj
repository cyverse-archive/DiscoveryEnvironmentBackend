(ns metadactyl.core
  (:gen-class)
  (:use [clojure.java.io :only [file]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [compojure.api.sweet]
        [compojure.core :only [GET PUT POST]]
        [metadactyl.beans]
        [metadactyl.kormadb]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.app.element]
        [metadactyl.routes.domain.pipeline]
        [metadactyl.routes.domain.tool-requests]
        [metadactyl.routes.params]
        [metadactyl.user :only [store-current-user]]
        [ring.middleware keyword-params nested-params]
        [ring.swagger.schema :only [describe]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [metadactyl.routes.admin :as admin-routes]
            [metadactyl.routes.apps :as app-routes]
            [metadactyl.routes.apps.categories :as app-category-routes]
            [metadactyl.routes.apps.elements :as app-element-routes]
            [metadactyl.routes.tool-requests :as tool-request-routes]
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
   (swaggered "app-categories"
              :description "Discovery Environment App Category endpoints."
              (context "/apps/categories" [:as {params :params}]
                       (store-current-user app-category-routes/app-categories params)))
   (swaggered "apps"
              :description "Discovery Environment App endpoints."
              (context "/apps" [:as {params :params}]
                       (store-current-user app-routes/apps params)))
   (swaggered "element-types"
              :description "Discovery Environment App Element endpoints."
              (context "/apps/elements" [:as {params :params}]
                       (store-current-user app-element-routes/app-elements params)))
   (swaggered "tool-requests"
              :description "Tool Request endpoints."
              (context "/tool-requests" [:as {params :params}]
                       (store-current-user tool-request-routes/tool-requests params)))
   (swaggered "admin-apps"
              :description "Admin App endpoints."
              (context "/admin/apps" [:as {params :params}]
                       (store-current-user admin-routes/admin-apps params)))
   (swaggered "admin-tool-requests"
              :description "Admin Tool Request endpoints."
              (context "/admin/tool-requests" [:as {params :params}]
                       (store-current-user admin-routes/tool-requests params)))
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
