(ns conrad.core
  (:gen-class)
  (:use [compojure.core]
        [ring.middleware keyword-params nested-params params]
        [clj-cas.cas-proxy-auth :only (validate-cas-group-membership)]
        [clojure-commons.query-params :only (wrap-query-params)]
        [conrad.app-admin]
        [conrad.category-admin]
        [conrad.genome-reference]
        [conrad.common]
        [conrad.kormadb]
        [conrad.config]
        [conrad.listings]
        [conrad.database]
        [clojure.java.io :only (file)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure-commons.clavin-client :as cl]
            [clojure-commons.props :as cp]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty])
  (:import [java.sql SQLException]))

(defn- trap
"An error catching function."
[f]
  (try
    (f)
    (catch IllegalArgumentException e (failure-response e))
    (catch IllegalStateException e (failure-response e))
    (catch SQLException e (do (log-next-exception e) (error-response e)))
    (catch Throwable t (error-response t))))

;; Secured routes.
(defroutes secured-routes
  (GET "/get-app-groups" []
       (trap #(get-public-categories)))

  (GET "/get-apps-in-group/:id" [id]
       (trap #(get-category-with-apps id)))

  (GET "/get-components-in-app/:id" [id]
       (trap #(get-components-in-app id)))

  (POST "/update-app" [:as {body :body}]
        (trap #(update-app body)))

  (POST "/rename-category" [:as {body :body}]
        (trap #(rename-category body)))

  (DELETE "/category/:id" [id]
          (trap #(delete-category id)))

  (PUT "/category" [:as {body :body}]
       (trap #(create-category body)))

  (DELETE "/app/:id" [id]
          (trap #(delete-app id)))

  (POST "/move-app" [:as {body :body}]
        (trap #(move-app body)))

  (GET "/undelete-app/:id" [id]
       (trap #(undelete-app id)))

  (POST "/move-category" [:as {body :body}]
        (trap #(move-category body)))

  (GET "/all-genome-references" []
       (trap #(get-all-genome-references)))

  (GET "/genome-references" []
       (trap #(get-genome-references)))

  (GET "/genome-references-by-user/:un" [un]
       (trap #(get-genome-references-by-username un)))

  (GET "/genome-reference/:id" [id]
       (trap #(get-genome-reference-by-uuid id)))

  (DELETE "/genome-reference/:id" [id]
       (trap #(delete-genome-reference-by-uuid id)))

  (PUT "/genome-reference" [:as {body :body attrs :user-attributes}]
       (trap #(insert-genome-reference body attrs)))

  (POST "/genome-reference" [:as {body :body attrs :user-attributes}]
       (trap #(modify-genome-reference body attrs)))

  (route/not-found (unrecognized-path-response)))

;; All routes.
(defroutes conrad-routes

  (GET "/" [] "Welcome to Conrad!\n")

  (context "/secured" []
           (validate-cas-group-membership
             secured-routes #(cas-server) #(server-name) #(group-attr-name)
             #(allowed-groups)))

  (route/not-found (unrecognized-path-response)))

(defn- log-props
  "Logs the configuration settings."
  []
  (dorun (map (fn [[k v]] (log/warn k "=" (if (re-find #"password" k) "****" v)))
              (sort-by key @props))))

(defn- validate-props
  "Validates the configuration settings."
  []
  (when (not (configuration-valid))
    (log/warn "THE CONFIGURATION IS INVALID - EXITING NOW")
    (System/exit 1)))

(defn- init-service
  "Initializes the service after the configuration settings have been loaded."
  []
  (log-props)
  (validate-props)
  (define-database))

(defn load-configuration-from-file
  "Loads the configuration settings from a properties file."
  []
  (let [filename "conrad.properties"
        conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (if (nil? conf-dir)
      (reset! props (cp/read-properties (file filename)))
      (reset! props (cp/read-properties (file conf-dir filename)))))
  (init-service))

(defn load-configuration-from-zookeeper
  "Loads the configuration properties from Zookeeper."
  []
  (cl/with-zk
    (zk-url)
    (when (not (cl/can-run?))
      (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
      (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY.")
      (System/exit 1))
    (reset! props (cl/properties "conrad")))
  (init-service))

(defn site-handler [routes]
  (-> routes
      wrap-keyword-params
      wrap-nested-params
      wrap-query-params))

(def app
  (site-handler conrad-routes))

(defn -main
  [& args]
  (load-configuration-from-zookeeper)
  (log/warn "Listening on" (listen-port))
  (jetty/run-jetty app {:port (listen-port)}))
