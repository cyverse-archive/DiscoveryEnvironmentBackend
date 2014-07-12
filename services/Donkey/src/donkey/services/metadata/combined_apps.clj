(ns donkey.services.metadata.combined-apps
  (:use [korma.db])
  (:require [donkey.persistence.apps :as ap]
            [donkey.services.metadata.agave-apps :as aa]
            [donkey.services.metadata.de-apps :as da]
            [donkey.util :as util]
            [donkey.util.db :as db]))

(defn app-step-partitioner
  "Partitions app steps into units of execution. Each external app step has to run by itself.
   Consecutive DE app steps can be combined into a single step."
  [{external-app-id :external_app_id step-number :step_number}]
  (when-not (nil? external-app-id)
    (str external-app-id "/" step-number)))

(defn load-app-steps
  "Loads the app steps from the database, grouping consecutive DE steps into a single step."
  [app-id]
  (->> (map (fn [step n] (assoc step :step_number n))
            (ap/load-app-steps app-id)
            (iterate inc 1))
       (partition-by app-step-partitioner)
       (map first)))

(defn submit-de-job
  "Submits a DE job to the remote system. A DE job is a job using any app defined in the DE
   database, which may consist of Agave steps, DE steps or both."
  [agave workspace-id app-id submission]
  (let [app-steps (load-app-steps app-id)]))

(defn submit-job
  "Submits a job for execution. The job may run exclusively in Agave, exclusively in the DE, or it
   may have steps that run on both systems."
  [agave workspace-id submission]
  (let [app-id (:analysis_id submission)]
    (if (util/is-uuid? app-id)
      (submit-de-job agave workspace-id app-id submission)
      (aa/submit-agave-job agave app-id))))
