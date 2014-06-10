(ns mescal.agave-de-v2
  (:require [mescal.agave-de-v2.apps :as apps]
            [mescal.agave-de-v2.app-listings :as app-listings]
            [mescal.agave-de-v2.job-params :as params]
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

(defn get-app-details
  [agave app-id]
  (apps/format-app-details (.getApp agave app-id)))

(defn get-app-deployed-component
  [agave app-id]
  (apps/format-deployed-component-for-app (.getApp agave app-id)))

(defn submit-job
  [agave irods-home submission]
  (let [app-id (:analysis_id submission)
        app    (.getApp agave app-id)]
    (->> (jobs/prepare-submission irods-home app submission)
         (.submitJob agave)
         (jobs/format-job irods-home true (get-system-statuses agave) {app-id app}))))

(defn- format-jobs
  [agave irods-home jobs-enabled? jobs]
  (let [app-info (apps/load-app-info agave (mapv :appId jobs))
        statuses (get-system-statuses agave)]
    (mapv (partial jobs/format-job irods-home jobs-enabled? statuses app-info) jobs)))

(defn list-jobs
  ([agave irods-home jobs-enabled?]
     (format-jobs agave irods-home jobs-enabled? (.listJobs agave)))
  ([agave irods-home jobs-enabled? job-ids]
     (format-jobs agave irods-home jobs-enabled? (.listJobs agave job-ids))))

(defn list-job
  [agave irods-home jobs-enabled? job-id]
  (let [job      (.listJob agave job-id)
        statuses (get-system-statuses agave)
        app-info (apps/load-app-info agave [(:appId job)])]
    (jobs/format-job irods-home jobs-enabled? statuses app-info job)))

(defn get-job-params
  [agave irods-home job-id]
  (when-let [job (.listJob agave job-id)]
    (params/format-params irods-home job (:appId job) (.getApp agave (:appId job)))))

(defn get-app-rerun-info
  [agave irods-home job-id]
  (when-let [job (.listJob agave job-id)]
    (apps/format-app-rerun-info irods-home (.getApp agave (:appId job)) job)))

(defn translate-job-status
  [status]
  (jobs/translate-job-status status))
