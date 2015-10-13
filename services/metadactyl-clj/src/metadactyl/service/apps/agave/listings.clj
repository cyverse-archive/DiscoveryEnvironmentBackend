(ns metadactyl.service.apps.agave.listings
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit uuid?]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-metadata :as ap]))

(defn list-apps
  [agave category-id params]
  (-> (.listApps agave)
      (sort-apps params {:default-sort-field "name"})
      (apply-offset params)
      (apply-limit params)))

(defn search-apps
  [agave search-term params]
  (try+
   (-> (.searchApps agave search-term)
       (sort-apps params {:default-sort-field "name"})
       (apply-offset params)
       (apply-limit params))
   (catch [:error_code ce/ERR_UNAVAILABLE] _
     (log/error (:throwable &throw-context) "Agave app search timed out")
     nil)
   (catch :status _
     (log/error (:throwable &throw-context) "HTTP error returned by Agave")
     nil)))

(defn load-app-tables
  [agave]
  (try+
   (->> (.listApps agave)
        (:apps)
        (map (juxt :id identity))
        (into {})
        (vector))
   (catch [:type :clojure-commons.exception/unavailable] _
     (log/warn (:throwable &throw-context) "Agave app table retrieval timed out")
     [])
   (catch :status _
     (log/error (:throwable &throw-context) "HTTP error returned by Agave")
     [])))

(defn- prep-agave-param
  [step-id agave-app-id param]
  (let [is-file-param? (re-find #"^(?:File|Folder)" (:type param))]
    {:data_format     (when is-file-param? "Unspecified")
     :info_type       (when is-file-param? "PlainText")
     :omit_if_blank   false
     :is_visible      (:isVisible param)
     :name            (:name param)
     :is_implicit     false
     :external_app_id agave-app-id
     :ordering        (:order param)
     :type            (:type param)
     :step_id         step-id
     :label           (:label param)
     :id              (:id param)
     :description     (:description param)
     :default_value   (:defaultValue param)}))

(defn- load-agave-pipeline-step-params
  [agave-client {step-id :step_id agave-app-id :external_app_id}]
  (->> (.getApp agave-client agave-app-id)
       (:groups)
       (mapcat :parameters)
       (map (partial prep-agave-param step-id agave-app-id))))

(defn- load-agave-pipeline-params
  [agave-client app-id]
  (->> (ap/load-app-steps app-id)
       (remove (comp nil? :external_app_id))
       (mapcat (partial load-agave-pipeline-step-params agave-client))))

(defn- load-agave-app-params
  [agave-client app-id]
  (->> (.getApp agave-client app-id)
       (:groups)
       (mapcat :parameters)
       (map (partial prep-agave-param nil app-id))))

(defn get-param-definitions
  [agave app-id]
  (if (uuid? app-id)
    (load-agave-pipeline-params agave app-id)
    (load-agave-app-params agave app-id)))
