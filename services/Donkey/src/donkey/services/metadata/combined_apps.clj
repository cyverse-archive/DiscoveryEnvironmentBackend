(ns donkey.services.metadata.combined-apps
  (:use [donkey.auth.user-attributes :only [current-user]]
        [korma.db]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.agave-apps :as aa]
            [donkey.services.metadata.de-apps :as da]
            [donkey.util :as util]
            [donkey.util.db :as db]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(defn- remove-mapped-inputs
  [mapped-props group]
  (assoc group :properties (remove (comp mapped-props :id) (:properties group))))

(defn- reformat-group
  [app-name step-name group]
  (assoc group
    :name       (str app-name " - " (:name group))
    :label      (str app-name " - " (:label group))
    :properties (mapv (fn [prop] (assoc prop :id (str step-name "_" (:id prop))))
                      (:properties group))))

(defn- get-agave-groups
  [agave step external-app-id]
  (let [app          (.getApp agave external-app-id)
        mapped-props (set (map :input_id (ap/load-target-step-mappings (:step_id step))))]
    (->> (:groups app)
         (map (partial remove-mapped-inputs mapped-props))
         (remove (comp empty? :properties))
         (map (partial reformat-group (:name app) (:step_name step)))
         (doall))))

(defn- get-combined-groups
  [agave app-id metadactyl-groups]
  (loop [acc               []
         metadactyl-groups metadactyl-groups
         [step & steps]    (ap/load-app-steps app-id)
         step-number       1]
    (let [before-current-step #(<= (:step_number %) step-number)
          external-app-id     (:external_app_id step)]
      (cond
       ;; We're out of steps.
       (nil? step)
       acc

       ;; The current step is an Agave step.
       external-app-id
       (recur (concat acc (get-agave-groups agave step external-app-id))
              metadactyl-groups
              steps
              (inc step-number))

       ;; The current step is a DE step.
       :else
       (recur (concat acc (take-while before-current-step metadactyl-groups))
              (drop-while before-current-step metadactyl-groups)
              steps
              (inc step-number))))))

(defn- get-combined-app
  [agave app-id]
  (let [metadactyl-app (metadactyl/get-app app-id)]
    (assoc metadactyl-app
      :groups (get-combined-groups agave app-id (:groups metadactyl-app)))))

(defn get-app
  [agave app-id]
  (if (util/is-uuid? app-id)
    (get-combined-app agave app-id)
    (.getApp agave app-id)))

(defn- current-timestamp
  []
  (tf/unparse (tf/formatter "yyyy-MM-dd-HH-mm-ss.S") (t/now)))

(defn- job-name-to-path
  "Converts a job name to a string suitable for inclusion in a path."
  [path]
  (string/replace path #"[\s@]" "_"))

(defn- app-step-partitioner
  "Partitions app steps into units of execution. Each external app step has to run by itself.
   Consecutive DE app steps can be combined into a single step."
  [{external-app-id :external_app_id step-number :app_step_number}]
  (when-not (nil? external-app-id)
    (str external-app-id "/" step-number)))

(defn- add-job-step-type
  "Determines the type of a job step."
  [job-step]
  (assoc job-step
    :job_type (if (nil? (:external_app_id job-step)) jp/agave-job-type jp/de-job-type)))

(defn- load-job-steps
  "Loads the app steps from the database, grouping consecutive DE steps into a single step."
  [app-id]
  (->> (map (fn [n step] (assoc step :app_step_number n))
            (iterate inc 1)
            (ap/load-app-steps app-id))
       (partition-by app-step-partitioner)
       (map first)
       (map (fn [n step] (assoc step :step_number n))
            (iterate inc 1))
       (map add-job-step-type)))

(defn- validate-job-steps
  "Verifies that at least one step is associated with a job submission."
  [app-id steps]
  (when (empty? steps)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     (str "app " app-id " has no steps")}))
  steps)

(defn- build-result-folder-path
  [submission]
  (let [build-path (comp ft/rm-last-slash ft/path-join)]
    (if (:create_output_subdir submission)
      (build-path (:output_dir submission)
                  (str (job-name-to-path (:name submission)) "-" (current-timestamp)))
      (build-path (:output_dir submission)))))

(defn- build-job-save-info
  [result-folder-path job-id app-info submission]
  {:id                 job-id
   :job-name           {:name submission}
   :description        {:description submission}
   :app-id             {:id app-info}
   :app-name           {:name app-info}
   :app-description    {:description app-info}
   :app-wiki-url       {:wikiurl app-info}
   :result-folder-path result-folder-path
   :start-date         (db/now)
   :status             "Submitted"
   :username           (:username current-user)})

(defn- build-job-step-save-info
  [job-id job-step]
  {:job-id          job-id
   :step-number     (:step_number job-step)
   :status          "Pending"
   :job-type        (:job_type job-step)
   :app-step-number (:app_step_number job-step)})

(defn- is-de-job-step?
  [job-step]
  (= (:job_type job-step) (jp/de-job-type)))

(defn- record-step-submission
  [external-id job-info job-step]
  (jp/update-job-step-number (:id job-info)
                             (:step-number job-step)
                             {:external-id external-id
                              :status      "Submitted"
                              :start-date  (db/now)}))

(defn- submit-job-step
  [agave workspace-id job-info job-step submission]
  (doto (if (is-de-job-step? job-step)
          (da/submit-job-step workspace-id job-info job-step submission)
          (aa/submit-job-step agave job-info job-step submission))
    (record-step-submission job-info job-step)))

(defn- submit-de-job
  "Submits a DE job to the remote system. A DE job is a job using any app defined in the DE
   database, which may consist of Agave steps, DE steps or both."
  [agave workspace-id app-id submission]
  (let [app-info  (service/assert-found (ap/load-app-info app-id) "app" app-id)
        job-id    (UUID/randomUUID)
        job-info  (build-job-save-info (build-result-folder-path submission)
                                       job-id app-info submission)
        job-steps (map (partial build-job-step-save-info job-id)
                       (validate-job-steps app-id (load-job-steps app-id)))]
    (jp/save-multistep-job job-info job-steps submission)
    (submit-job-step agave workspace-id job-info (first job-steps) submission)))

(defn submit-job
  "Submits a job for execution. The job may run exclusively in Agave, exclusively in the DE, or it
   may have steps that run on both systems."
  [agave workspace-id submission]
  (let [app-id (:analysis_id submission)]
    (if (util/is-uuid? app-id)
      (submit-de-job agave workspace-id app-id submission)
      (aa/submit-agave-job agave app-id))))
