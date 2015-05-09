(ns donkey.services.metadata.combined-apps
  (:use [donkey.auth.user-attributes :only [current-user]]
        [korma.db]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.jex :as jex]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.persistence.workspaces :as wp]
            [donkey.services.metadata.util :as mu]
            [donkey.util.service :as service]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]))

(defn- find-incomplete-job-steps
  "Finds the list of incomplete job steps associated with a job. An empty list is returned if the
   job has no incomplete steps."
  [job-id]
  (remove (comp mu/is-completed? :status) (jp/list-job-steps job-id)))

(defn- stop-job-step
  "Stops an individual step in a job."
  [agave {:keys [id] :as job} [& steps]]
  (let [{:keys [external-id job-type] :as step} (first steps)]
    (when-not (string/blank? external-id)
      (if (= job-type jp/de-job-type)
        (jex/stop-job external-id)
        (when-not (nil? agave) (.stopJob agave external-id)))
      (jp/cancel-job-step-numbers id (mapv :step-number steps))
      (mu/send-job-status-notification job jp/canceled-status (db/now)))))

(defn stop-job
  "Stops a job. This function updates the database first in order to minimize the risk of a race
   condition; subsequent job steps should not be submitted once a job has been stopped. After the
   database has been updated, it calls the appropriate execution host to stop the currently running
   job step if one exists."
  ([job]
     (stop-job nil job))
  ([agave {:keys [id] :as job}]
     (jp/update-job id jp/canceled-status (db/now))
     (try+
      (stop-job-step agave job (find-incomplete-job-steps id))
      (catch Throwable t
        (log/warn t "unable to cancel the most recent step of job, " id))
      (catch Object _
        (log/warn "unable to cancel the most recent step of job, " id)))))
