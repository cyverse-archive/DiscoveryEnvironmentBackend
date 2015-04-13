(ns metadactyl.service.apps.combined.util
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit uuid?]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]))

(defn apply-default-search-params
  [params]
  (assoc params
    :sort-field (or (:sort-field params) "name")
    :sort-dir   (or (:sort-dir params) "ASC")))

(defn combine-app-search-results
  [params results]
  (let [params (apply-default-search-params params)]
    (-> {:app_count (apply + (map :app_count results))
         :apps      (mapcat :apps results)}
        (sort-apps params)
        (apply-offset params)
        (apply-limit params))))

(defn get-apps-client
  ([clients]
     (or (first (filter #(.canEditApps %) clients))
         (throw+ {:error_code ce/ERR_BAD_REQUEST
                  :reason     "apps are not editable at this time."})))
  ([clients client-name]
     (or (first (filter #(= client-name (.getClientName %)) clients))
         (throw+ {:error_code ce/ERR_BAD_REQUEST
                  :reason     (str "unrecognized client name " client-name)}))))

(defn apps-client-for-job
  [{app-id :app_id :as submission} clients]
  (cond (not (uuid? app-id))                     (get-apps-client clients jp/agave-client-name)
        (zero? (ap/count-external-steps app-id)) (get-apps-client clients jp/de-client-name)
        :else                                    nil))
