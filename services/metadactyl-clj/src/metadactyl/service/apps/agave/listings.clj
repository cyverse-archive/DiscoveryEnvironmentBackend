(ns metadactyl.service.apps.agave.listings
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit uuid?]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-metadata :as ap]))

(defn list-apps
  [agave category-id params]
  (-> (.listApps agave)
      (sort-apps params)
      (apply-offset params)
      (apply-limit params)))

(defn search-apps
  [agave search-term params]
  (try+
   (-> (.searchApps agave search-term)
       (sort-apps params)
       (apply-offset params)
       (apply-limit params))
   (catch [:error_code ce/ERR_UNAVAILABLE] _ nil)))

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

(defn- load-agave-app-params
  [agave-client {step-id :step_id agave-app-id :external_app_id}]
  (->> (.getApp agave-client agave-app-id)
       (:groups)
       (mapcat :parameters)
       (map (partial prep-agave-param step-id agave-app-id))))

(defn- load-agave-pipeline-params
  [agave-client app-id]
  (->> (ap/load-app-steps app-id)
       (remove (comp nil? :external_app_id))
       (mapcat (partial load-agave-app-params agave-client))))

(defn get-param-definitions
  [agave app-id]
  (if (uuid? app-id)
    (load-agave-pipeline-params agave app-id)
    (load-agave-app-params agave app-id)))
