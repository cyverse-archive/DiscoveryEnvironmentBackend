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
            [donkey.services.metadata.agave-apps :as aa]
            [donkey.services.metadata.de-apps :as da]
            [donkey.services.metadata.util :as mu]
            [donkey.services.metadata.property-values :as property-values]
            [donkey.util.service :as service]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]))

(defn- remove-mapped-inputs
  [mapped-props group]
  (assoc group :parameters (remove (comp mapped-props :id) (:parameters group))))

(defn- reformat-group
  [app-name step-id group]
  (assoc group
    :name       (str app-name " - " (:name group))
    :label      (str app-name " - " (:label group))
    :parameters (mapv (fn [prop] (assoc prop :id (str step-id "_" (:id prop))))
                      (:parameters group))))


(defn- get-mapped-props
  [step-id]
  (->> (ap/load-target-step-mappings step-id)
       (map (fn [{ext-id :external_input_id id :input_id}]
              (str (first (remove nil? [ext-id id])))))
       (set)))

(defn- get-agave-groups
  [agave step external-app-id]
  (mu/assert-agave-enabled agave)
  (let [app          (.getApp agave external-app-id)
        mapped-props (get-mapped-props (:step_id step))]
    (->> (:groups app)
         (map (partial remove-mapped-inputs mapped-props))
         (remove (comp empty? :parameters))
         (map (partial reformat-group (:name app) (:step_id step)))
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
  (let [metadactyl-app (service/decode-json (:body (metadactyl/get-app app-id)))]
    (->> (:groups metadactyl-app)
         (get-combined-groups agave app-id)
         (assoc metadactyl-app :groups))))

(defn get-app
  [agave app-id]
  (if (uuids/is-uuid? app-id)
    (get-combined-app agave app-id)
    (do (mu/assert-agave-enabled agave)
        (.getApp agave app-id))))

(defn- prepare-pipeline-step
  "Prepares a single step in a pipeline for submission to metadactyl. DE steps can be left as-is.
   External steps need to have the task_id field moved to the external_app_id field."
  [{app-type :app_type :as step}]
  (if (= app-type "External")
    (assoc (dissoc step :task_id) :external_app_id (:task_id step))
    (update-in )))

(defn update-pipeline
  [agave app-id pipeline]
  (->> (update-in pipeline [:steps] (partial map prepare-pipeline-step))
       (metadactyl/update-pipeline app-id)
       (aa/format-pipeline-tasks agave)))

(defn- get-job-submission-config
  [job]
  (let [submission (:submission job)]
    (when-not submission
      (throw+ {:error_code ce/ERR_NOT_FOUND
               :reason     "Job submission values could not be found."}))
    (:config (service/decode-json (.getValue submission)))))

(defn get-job-params
  [agave-client job]
  (property-values/format-job-params agave-client
                                     (:app-id job)
                                     (:id job)
                                     (get-job-submission-config job)))

(defn get-app-rerun-info
  "Updates an app with the parameter values from a previous experiment plugged into the appropriate
   parameters."
  [agave-client job]
  (let [app           (get-app agave-client (:app-id job))
        values        (get-job-submission-config job)
        update-prop   #(let [id (keyword (:id %))]
                         (if (contains? values id)
                           (assoc %
                             :value        (values id)
                             :defaultValue (values id))
                           %))
        update-props  #(map update-prop %)
        update-group  #(update-in % [:parameters] update-props)
        update-groups #(map update-group %)]
    (update-in app [:groups] update-groups)))

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
