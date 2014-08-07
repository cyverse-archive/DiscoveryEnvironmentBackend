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
            [donkey.util :as util]
            [donkey.util.db :as db]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(declare submit-next-step)

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
  (mu/assert-agave-enabled agave)
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
    (->> (:groups metadactyl-app)
         (get-combined-groups agave app-id)
         (assoc metadactyl-app :groups))))

(defn get-app
  [agave app-id]
  (if (util/is-uuid? app-id)
    (get-combined-app agave app-id)
    (do (mu/assert-agave-enabled agave)
        (.getApp agave app-id))))

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
    :job_type (if (nil? (:external_app_id job-step)) jp/de-job-type jp/agave-job-type)))

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

(defn- build-job-save-info
  [result-folder-path job-id app-info submission]
  {:id                 job-id
   :job-name           (:name submission)
   :description        (:description submission)
   :app-id             (:id app-info)
   :app-name           (:name app-info)
   :app-description    (:description app-info)
   :app-wiki-url       (:wikiurl app-info)
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
  (= (:job-type job-step) jp/de-job-type))

(defn- record-step-submission
  [external-id job-info job-step]
  (jp/update-job-step-number (:id job-info)
                             (:step-number job-step)
                             {:external-id external-id
                              :status      "Submitted"
                              :start-date  (db/now)}))

(defn- submit-job-step
  [agave workspace-id job-info {:keys [app-step-number] :as job-step} submission]
  (doto (if (is-de-job-step? job-step)
          (let [output-dir (:result-folder-path job-info)
                submission (assoc (mu/update-submission-result-folder submission output-dir)
                             :starting_step app-step-number)]
            (da/submit-job-step workspace-id job-info job-step submission))
          (let [app-steps     (ap/load-app-steps (:app-id job-info))
                curr-app-step (nth app-steps (dec app-step-number))
                output-dir    (:result-folder-path job-info)
                submission    (assoc (mu/update-submission-result-folder submission output-dir)
                                :analysis_id (:external_app_id curr-app-step)
                                :paramPrefix (:step_name curr-app-step))]
            (aa/submit-job-step agave job-info job-step submission)))
    (record-step-submission job-info job-step)))

(defn- submit-de-job
  "Submits a DE job to the remote system. A DE job is a job using any app defined in the DE
   database, which may consist of Agave steps, DE steps or both."
  [agave workspace-id app-id submission]
  (let [app-info  (service/assert-found (ap/load-app-info app-id) "app" app-id)
        job-id    (UUID/randomUUID)
        job-info  (build-job-save-info (mu/build-result-folder-path submission)
                                       job-id app-info submission)
        job-steps (map (partial build-job-step-save-info job-id)
                       (validate-job-steps app-id (load-job-steps app-id)))]
    (jp/save-multistep-job job-info job-steps submission)
    (submit-job-step agave workspace-id job-info (first job-steps) submission)
    {:id         job-id
     :name       (:job-name job-info)
     :status     (:status job-info)
     :start-date (db/millis-from-timestamp (:start-date job-info))}))

(defn submit-job
  "Submits a job for execution. The job may run exclusively in Agave, exclusively in the DE, or it
   may have steps that run on both systems."
  [agave workspace-id submission]
  (let [app-id (:analysis_id submission)]
    (if (util/is-uuid? app-id)
      (submit-de-job agave workspace-id app-id submission)
      (aa/submit-agave-job agave submission))))

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
        update-group  #(update-in % [:properties] update-props)
        update-groups #(map update-group %)]
    (update-in app [:groups] update-groups)))

(defn- translate-job-status
  "Translates an Agave status code to something more consistent with the DE's status codes."
  [agave {:keys [job-type]} status]
  (if (= jp/agave-job-type job-type)
    (or (.translateJobStatus agave status) status)
    status))

(defn- get-default-output-name
  "Determines the default name of a job output."
  [agave source-id output-id app-steps]
  (let [step (first (filter #(= source-id (:step_id %)) app-steps))]
    (if (:external_app_id step)
      (.getDefaultOutputName agave (:external_app_id step) output-id)
      (ap/get-default-output-name (:template_id step) output-id))))

(defn- get-input-path
  "Determines the path of a mapped input."
  [agave job config app-steps input]
  (let [source-id        (:source_id input)
        source-name      (:source_name input)
        output-id        (:output_id input)
        config-output-id (str source-name "_" output-id)]
    (if-let [prop-value (get config config-output-id)]
      (ft/path-join (:result-folder-path job) prop-value)
      (ft/path-join (:result-folder-path job)
                    (get-default-output-name agave source-id output-id app-steps)))))

(defn- add-mapped-inputs
  "Adds the mapped inputs to the job submission for the next step."
  [agave job submission app-steps mapped-inputs]
  (update-in submission [:config]
             (fn [config]
               (reduce (fn [config input]
                         (assoc config
                           (keyword (str (:target_name input) "_" (:input_id input)))
                           (get-input-path agave job config app-steps input)))
                       config mapped-inputs))))

(defn update-job-status
  "Updates the status of a job. The job may have multiple steps, so the overall job status is only
   only changed when first step changes to any status up to Running, the last step changes to any
   status after Running, or the status of any step changes to Failed."
  [agave username job job-step status end-time]
  (let [{job-id :id}                      job
        {:keys [external-id step-number]} job-step
        max-step                          (jp/get-max-step-number job-id)
        first-step?                       (= step-number 1)
        last-step?                        (= step-number max-step)
        status                            (translate-job-status agave job-step status)]
    (if (mu/is-completed? (:status job))
      (log/warn (str "received a job status update for completed or canceled job, " job-id))
      (when-not (= status (:status job-step))
        (jp/update-job-step job-id external-id status end-time)
        (when (or (and first-step? (mu/not-completed? status))
                  (and last-step? (mu/is-completed? status))
                  (= status mu/failed-status))
          (jp/update-job job-id status end-time)
          (mu/send-job-status-notification job job-step status end-time))
        (when (and (not last-step?) (= status mu/completed-status))
          (submit-next-step agave username job job-step))))))

(defn- submit-next-step
  "Submits the next step in a job pipeline."
  [agave username job {:keys [job-id step-number] :as job-step}]
  (let [next-step-number (inc step-number)
        next-step        (jp/get-job-step-number job-id next-step-number)
        app-step-number  (:app-step-number next-step)
        app-steps        (ap/load-app-steps (:app-id job))
        next-app-step    (nth app-steps (dec app-step-number))
        mapped-inputs    (ap/load-target-step-mappings (:step_id next-app-step))
        submission       (service/decode-json (.getValue (:submission job)))
        submission       (add-mapped-inputs agave job submission app-steps mapped-inputs)
        workspace-id     (:id (wp/workspace-for-user username))]
    (try+
     (submit-job-step agave workspace-id job next-step submission)
     (catch Object o
       (log/warn (str "unable to submit the next step in job " (:id job)))
       (update-job-status agave username job next-step mu/failed-status (db/now))
       (throw+)))))

(defn- find-first-incomplete-job-step
  "Finds the first incomplete job step associated with a job. Nil is returned if the job has no
   incomplete steps."
  [job-id]
  (->> (jp/list-job-steps job-id)
       (remove (comp mu/is-completed? :status))
       (first)))

(defn- get-job-step-status
  [agave {:keys [job-type external-id]}]
  (if (= job-type jp/agave-job-type)
    (select-keys (.listJob agave external-id) [:status :enddate])
    (da/get-job-step-status external-id)))

(defn sync-job-status
  "Synchronizes the status of a job with the remote system."
  [agave {:keys [id username] :as job}]
  (if-let [step (find-first-incomplete-job-step id)]

    ;; We found an incomplete step to update.
    (if-let [step-status (get-job-step-status agave step)]
      (let [status   (:status step-status)
            end-date (db/timestamp-from-str (:enddate step-status))]
        (update-job-status agave username job step status end-date))
      (update-job-status agave username job step mu/failed-status (db/now)))

    ;; We didn't find an incomplete step to update.
    (let [steps         (jp/list-job-steps id)
          all-complete? (every? mu/is-completed? (map :status steps))
          status        (if all-complete? mu/completed-status mu/failed-status)]
      (jp/update-job id {:status status :end-date (db/now)}))))

(defn- stop-job-step
  "Stops an individual step in a job."
  [agave job-id {:keys [external-id job-type]}]
  (when-not (string/blank? external-id)
    (if (= job-type jp/de-job-type)
      (jex/stop-job external-id)
      (when-not (nil? agave) (.stopJob agave external-id)))
    (jp/update-job-step job-id external-id mu/canceled-status (db/now))))

(defn stop-job
  "Stops a job. This function updates the database first in order to minimize the risk of a race
   condition; subsequent job steps should not be submitted once a job has been stopped. After the
   database has been updated, it calls the appropriate execution host to stop the currently running
   job step if one exists."
  ([job-id]
     (stop-job nil job-id))
  ([agave job-id]
     (jp/update-job job-id mu/canceled-status (db/now))
     (try+
      (stop-job-step agave job-id (find-first-incomplete-job-step job-id))
      (catch Throwable t
        (log/warn t "unable to cancel the most recent step of job, " job-id))
      (catch Object _
        (log/warn "unable to cancel the most recent step of job, " job-id)))))
