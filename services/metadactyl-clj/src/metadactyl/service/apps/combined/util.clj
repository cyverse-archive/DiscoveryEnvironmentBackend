(ns metadactyl.service.apps.combined.util
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]))

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
  [clients]
  (or (first (filter #(.canEditApps %) clients))
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "apps are not editable at this time."})))
