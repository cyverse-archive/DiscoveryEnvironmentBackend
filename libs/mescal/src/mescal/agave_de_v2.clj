(ns mescal.agave-de-v2
  (:require [mescal.agave-de-v2.apps :as apps]
            [mescal.agave-de-v2.app-listings :as app-listings]
            [mescal.agave-de-v2.jobs :as jobs]))

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

(defn submit-job
  [agave irods-home submission]
  (let [app-id (:analysis_id submission)
        app    (.getApp agave app-id)]
    (->> (jobs/prepare-submission irods-home app submission)
         (.submitJob agave)
         (jobs/format-job irods-home true (get-system-statuses agave) {app-id app}))))
