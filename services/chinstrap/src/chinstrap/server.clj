(ns chinstrap.server
  (:gen-class)
  (:use [clojure.tools.cli :only [parse-opts]]
        [chinstrap.config :only [listen-port]]
        [chinstrap.db]
        [compojure.core])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [chinstrap.models.ajax-endpoints :as ajax]
            [chinstrap.views.pages :as pages]
            [compojure.route :as route]
            [noir.util.middleware :as nm]
            [ring.adapter.jetty :as jetty]))

(def ^:private app-routes
  [(GET "/de-analytics" []
        (pages/de-analytics-page))

   (GET "/de-analytics/info" []
        (pages/de-analytics-info-page))

   (GET "/de-analytics/apps" []
        (pages/de-analytics-apps-page))

   (GET "/de-analytics/components" []
        (pages/de-analytics-components-page))

   (GET "/de-analytics/integrators" []
        (pages/de-analytics-integrators-page))

   (GET "/de-analytics/graph" []
        (pages/de-analytics-graph-page))

   (GET "/de-analytics/graph/day" []
        (pages/de-analytics-graph-day-page))

   (GET "/de-analytics/graph/month" []
        (pages/de-analytics-graph-month-page))

   (GET "/de-analytics/raw" []
        (pages/de-analytics-raw-page))

   (GET "/de-analytics/get-day-data/" []
        (ajax/get-day-data))

   (GET "/de-analytics/get-month-data/" []
        (ajax/get-month-data))

   (GET "/de-analytics/get-day-data/:status" [status]
        (ajax/get-day-data-for status))

   (GET "/de-analytics/get-month-data/:status" [status]
        (ajax/get-month-data-for status))

   (GET "/de-analytics/get-info/:date" [date]
        (ajax/get-date-info date))

   (GET "/de-analytics/get-integrator-data/:id" [id]
        (ajax/get-integrator-data-for id))

   (GET "/de-analytics/get-integrator-data/" []
        (ajax/get-integrator-data))

   (GET "/de-analytics/get-apps" []
        (ajax/get-apps))

   (GET "/de-analytics/pending-analyses-by-user" []
        (ajax/get-pending-analyses-by-user))

   (GET "/de-analytics/get-components" []
        (ajax/get-components))

   (GET "/de-analytics/get-historical-app-count" []
        (ajax/get-historical-app-count))

   (route/not-found "Not Found")])

(def ^:private app
  (nm/app-handler app-routes))

(def ^:private cli-options
  [["-c" "--config PATH" "The path to the configuration file."
    :default  "/etc/iplant/de/chinstrap.properties"]
   ["-h" "--help" "Display a short help message."]])

(defn- usage
  [options-summary]
  (->> ["Discovery Environment status and usage metrics service."
        ""
        "Options:"
        options-summary]
       (string/join "\n")))

(defn- error-msg
  [errors]
  (str "Invalid command usage:\n\n"
       (string/join "\n" errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn launch
  [{:keys [config]}]
  (db-config config)
  (jetty/run-jetty app {:port (listen-port)}))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond (:help options) (exit 0 (usage summary))
          errors          (exit 1 (error-msg errors))
          :else           (launch options))))
