(ns metadactyl.service.apps.combined.util
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit]])
  (:require [clojure.tools.logging :as log]))

(defn apply-default-search-params
  [params]
  (assoc params
    :sort-field (or (:sort-field params) "name")
    :sort-dir   (or (:sort-dir params) "ASC")))

(defn combine-app-search-results
  [results params]
  (log/spy :warn params)
  (let [params (apply-default-search-params params)]
    (-> {:app_count (apply + (map :app_count results))
         :apps      (mapcat :apps results)}
        (sort-apps params)
        (apply-offset params)
        (apply-limit params))))
