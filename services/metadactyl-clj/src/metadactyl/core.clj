(ns metadactyl.core
  (:gen-class)
  (:use [clojure.java.io :only [file]]
        [clojure-commons.query-params :only [wrap-query-params]]
        [metadactyl.app-categorization]
        [metadactyl.app-listings]
        [metadactyl.app-validation :only [app-publishable?]]
        [metadactyl.beans]
        [metadactyl.collaborators]
        [metadactyl.kormadb]
        [metadactyl.metadata.element-listings :only [list-elements]]
        [metadactyl.metadata.tool-requests]
        [metadactyl.user :only [current-user store-current-user]]
        [metadactyl.util.service]
        [metadactyl.zoidberg]
        [ring.middleware keyword-params nested-params]
        [slingshot.slingshot :only [throw+]])
  (:require [compojure.api.sweet :refer :all]
            [compojure.core :refer [GET PUT POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.service.app-metadata :as app-metadata]
            [metadactyl.util.config :as config]
            [ring.adapter.jetty :as jetty]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]))

(defroutes* secured-routes
  (GET "/bootstrap" [:as {params :params headers :headers}]
       (ce/trap "bootstrap" #(throw+ '("bootstrap" (:ip-address params) (headers "user-agent")))))

  (GET "/logout" [:as {params :params}]
       (ce/trap "logout" #(throw+ '("logout" params))))

  (GET "/template/:app-id" [app-id]
       (throw+ '("get-app" app-id)))

  (GET "/app/:app-id" [app-id]
       (ce/trap "app" #(throw+ '("get-app-new-format" app-id))))

  (PUT "/workspaces/:workspace-id/newexperiment" [workspace-id :as {body :body}]
       (throw+ '("run-experiment" body workspace-id)))

  (POST "/rate-analysis" [:as {body :body}]
        (throw+ '("rate-app" body)))

  (POST "/delete-rating" [:as {body :body}]
        (throw+ '("delete-rating" body)))

  (GET "/search-analyses" [:as {params :params}]
       (search-apps params))

  (GET "/app-groups" [:as {params :params}]
       (trap #(get-only-app-groups params)))

  (GET "/get-analyses-in-group/:app-group-id"
       [app-group-id :as {params :params}]
       (list-apps-in-group app-group-id params))

  (GET "/get-components-in-analysis/:app-id" [app-id]
       (throw+ '("list-deployed-components-in-app" app-id)))

  (POST "/update-favorites" [:as {body :body}]
        (throw+ '("update-favorites" body)))

  (GET "/edit-template/:app-id" [app-id]
       (throw+ '("edit-app" app-id)))

  (GET "/edit-app/:app-id" [app-id]
       (throw+ '("edit-app-new-format" app-id)))

  (GET "/edit-workflow/:app-id" [app-id]
       (edit-workflow app-id))

  (GET "/copy-template/:app-id" [app-id]
       (throw+ '("copy-app" app-id)))

  (GET "/copy-workflow/:app-id" [app-id]
       (copy-workflow app-id))

  (PUT "/update-template" [:as {body :body}]
       (trap #(throw+ '("update-template-secured" body))))

  (PUT "/update-app" [:as {body :body}]
       (ce/trap "update-app" #(throw+ '("update-app-secured" body))))

  (POST "/update-workflow" [:as {body :body}]
        (trap #(throw+ '("update-workflow" body))))

  (POST "/make-analysis-public" [:as {body :body}]
        (trap #(throw+ '("make-app-public" body))))

  (GET "/is-publishable/:app-id" [app-id]
       (ce/trap "is-publishable"
                (fn [] {:publishable (first (app-publishable? app-id))})))

  (POST "/permanently-delete-workflow" [:as {body :body}]
        (ce/trap "permanently-delete-workflow" #(app-metadata/permanently-delete-apps body)))

  (POST "/delete-workflow" [:as {body :body}]
        (ce/trap "delete-workflow" #(app-metadata/delete-apps body)))

  (GET "/collaborators" [:as {params :params}]
       (get-collaborators params))

  (POST "/collaborators" [:as {params :params body :body}]
        (add-collaborators params (slurp body)))

  (POST "/remove-collaborators" [:as {params :params body :body}]
        (remove-collaborators params (slurp body)))

  (GET "/reference-genomes" []
       (throw+ '("list-reference-genomes")))

  (PUT "/reference-genomes" [:as {body :body}]
       (throw+ '("replace-reference-genomes" (slurp body))))

  (PUT "/tool-request" [:as {body :body}]
       (submit-tool-request (:username current-user) body))

  (POST "/tool-request" [:as {body :body}]
        (update-tool-request (config/uid-domain) (:username current-user) body))

  (GET "/tool-requests" [:as {:keys [params]}]
       (list-tool-requests (assoc params :username (:username current-user))))

  (route/not-found (unrecognized-path-response)))

(defroutes* metadactyl-routes
  (GET "/" []
       "Welcome to Metadactyl!\n")

  (GET "/get-workflow-elements/:element-type" [element-type :as {params :params}]
       (trap #(success-response (list-elements element-type params))))

  (GET "/search-deployed-components/:search-term" [search-term]
       (trap #(throw+ '("search-deployed-components" search-term))))

  (GET "/get-all-analysis-ids" []
       (trap #(get-all-app-ids)))

  (POST "/delete-categories" [:as {body :body}]
        (trap #(throw+ '("delete-categories" body))))

  (GET "/validate-analysis-for-pipelines/:app-id" [app-id]
       (trap #(throw+ '("validate-app-for-pipelines" app-id))))

  (GET "/apps/:app-id/data-objects" [app-id]
       (trap #(throw+ '("get-data-objects-for-app" app-id))))

  (POST "/categorize-analyses" [:as {body :body}]
        (trap #(categorize-apps body)))

  (GET "/get-analysis-categories/:category-set" [category-set]
       (trap #(throw+ '("get-app-categories" category-set))))

  (POST "/can-export-analysis" [:as {body :body}]
        (trap #(throw+ '("can-export-app" body))))

  (POST "/add-analysis-to-group" [:as {body :body}]
        (trap #(throw+ '("add-app-to-group" body))))

  (GET "/get-analysis/:app-id" [app-id]
       (trap #(throw+ '("get-app" app-id))))

  (GET "/analysis-details/:app-id" [app-id]
       (trap #(get-app-details app-id)))

  (GET "/public-app-groups" [:as {params :params}]
       (trap #(get-public-app-groups params)))

  (GET "/list-analysis/:app-id" [app-id]
       (throw+ '("list-app" app-id)))

  (GET "/export-template/:template-id" [template-id]
       (trap #(throw+ '("export-template" template-id))))

  (GET "/export-workflow/:app-id" [app-id]
       (trap #(throw+ '("export-workflow" app-id))))

  (POST "/export-deployed-components" [:as {body :body}]
        (trap #(throw+ '("export-deployed-components" body))))

  (POST "/preview-template" [:as {body :body}]
        (trap #(throw+ '("preview-template" body))))

  (POST "/preview-workflow" [:as {body :body}]
        (trap #(throw+ '("preview-workflow" body))))

  (POST "/update-template" [:as {body :body}]
        (trap #(throw+ '("update-template" body))))

  (POST "/force-update-workflow" [:as {body :body params :params}]
        (trap #(throw+ '("force-update-workflow" body params))))

  (POST "/update-workflow" [:as {body :body}]
        (trap #(throw+ '("update-workflow" body))))

  (POST "/import-template" [:as {body :body}]
        (trap #(throw+ '("import-template" body))))

  (POST "/import-workflow" [:as {body :body}]
        (trap #(throw+ '("import-workflow" body))))

  (POST "/import-tools" [:as {body :body}]
        (trap #(throw+ '("import-tools" body))))

  (POST "/update-analysis" [:as {body :body}]
        (trap #(throw+ '("update-app" body))))

  (POST "/update-app-labels" [:as {body :body}]
        (ce/trap "update-app-labels" #(app-metadata/relabel-app body)))

  (GET "/get-app-description/:app-id" [app-id]
       (trap #(get-app-description app-id)))

  (POST "/tool-request" [:as {body :body}]
        (trap #(update-tool-request (config/uid-domain) body)))

  (GET "/tool-request/:uuid" [uuid]
       (trap #(get-tool-request uuid)))

  (GET "/tool-requests" [:as {params :params}]
       (trap #(list-tool-requests params)))

  (GET "/tool-request-status-codes" [:as {params :params}]
       (trap #(list-tool-request-status-codes params)))

  (POST "/arg-preview" [:as {body :body}]
        (ce/trap "arg-preview" #(app-metadata/preview-command-line body)))

  (route/not-found (unrecognized-path-response)))

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

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-query-params))

(defapi app
  (swagger-ui "/api-ui" )
  (swagger-docs "/api/api-docs"
                :title "Metadactyl API"
                :description "Documentation for the Metadactyl REST API"
                :apiVersion "0.0.2")
  (swaggered "unsecured"
             :description "Discovery Environment App endpoints."
             (site-handler metadactyl-routes))
  (swaggered "secured"
             :description "Secured Discovery Environment App endpoints."
             (site-handler
              (context "/secured" [:as {params :params}]
                       (store-current-user secured-routes params)))))

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
