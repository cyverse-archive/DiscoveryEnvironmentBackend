(ns mescal.agave-de-v2
  (:require [mescal.agave-de-v2.apps :as apps]
            [mescal.agave-de-v2.app-listings :as app-listings]))

(defn hpc-app-group
  []
  (app-listings/hpc-app-group))

(defn- get-system-statuses
  [agave]
  (into {} (map (juxt :id :status) (.listSystems agave))))

(defn list-apps
  [agave jobs-enabled?]
  (app-listings/list-apps agave (get-system-statuses agave) jobs-enabled?))

(defn get-app
  [agave app-id]
  (apps/format-app (.getApp agave app-id)))
