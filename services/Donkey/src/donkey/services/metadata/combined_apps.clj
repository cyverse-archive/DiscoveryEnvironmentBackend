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
            [donkey.util.service :as service]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]))

(declare submit-next-step)

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
  (if (util/is-uuid? app-id)
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
   :app-wiki-url       (:wiki_url app-info)
   :result-folder-path result-folder-path
   :start-date         (db/now)
   :status             "Submitted"
   :username           (:username current-user)
   :notify             (:notify submission false)})

(defn- build-job-step-save-info
  [job-id job-step]
  {:job-id          job-id
   :step-number     (:step_number job-step)
   :status          jp/pending-status
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

(defn- submit-de-job-step
  [job-info {:keys [app-step-number] :as job-step} submission]
  (let [output-dir (ft/build-result-folder-path submission)
        submission (assoc (mu/update-submission-result-folder submission output-dir)
                     :starting_step app-step-number)
        external-id (da/submit-job-step submission)]
    (when-not (nil? job-info)
      (record-step-submission external-id job-info job-step))
    external-id))

(defn- submit-agave-job-step
  [agave job-info {:keys [app-step-number] :as job-step} submission]
  (let [app-steps     (ap/load-app-steps (:app-id job-info))
        curr-app-step (nth app-steps (dec app-step-number))
        output-dir    (:result-folder-path job-info)
        submission    (assoc (mu/update-submission-result-folder submission output-dir)
                        :app_id (:external_app_id curr-app-step)
                        :paramPrefix (:step_id curr-app-step))
        external-id (aa/submit-job-step agave job-info job-step submission)]
    (record-step-submission external-id job-info job-step)
    external-id))

(defn- submit-job-step
  [agave job-info job-step submission]
  (if (is-de-job-step? job-step)
    (submit-de-job-step job-info job-step submission)
    (submit-agave-job-step agave job-info job-step submission)))

(defn- submit-combined-job
  "Submits a DE job to the remote system. A DE job is a job using any app defined in the DE
   database, which may consist of Agave steps, DE steps or both."
  [agave app-id job-id job-steps submission]
  (let [app-info  (service/assert-found (ap/load-app-info app-id) "app" app-id)
        job-info  (build-job-save-info (ft/build-result-folder-path submission)
                                       job-id app-info submission)]
    (jp/save-multistep-job job-info job-steps submission)
    (submit-job-step agave job-info (first job-steps) submission)
    (mu/send-job-status-notification job-info jp/submitted-status nil)
    {:id         job-id
     :name       (:job-name job-info)
     :status     (:status job-info)
     :start-date (db/millis-from-timestamp (:start-date job-info))}))

(defn- submit-de-only-job
  "Submits a DE-only job to the remote system. A DE-only job is a job using any app defined in the
   DE database and consists only of DE steps."
  [job-step submission]
  (let [uuid     (submit-de-job-step nil job-step submission)
        job-id   ((comp :job-id first) (jp/get-job-steps-by-external-id uuid))
        job-info (jp/get-job-by-id job-id)]
    (mu/send-job-status-notification job-info jp/submitted-status nil)
    {:id         (:id job-info)
     :name       (:job-name job-info)
     :status     (:status job-info)
     :start-date (db/millis-from-timestamp (:start-date job-info))}))

(defn- submit-de-job
  "Submits a DE-only job or a DE job composed of Agave steps, DE steps or both."
  [agave app-id submission]
  (let [job-id    (uuids/uuid)
        job-steps (map (partial build-job-step-save-info job-id)
                       (validate-job-steps app-id (load-job-steps app-id)))]
    (if (every? is-de-job-step? job-steps)
      (submit-de-only-job (first job-steps) submission)
      (submit-combined-job agave app-id job-id job-steps submission))))

(defn submit-job
  "Submits a job for execution. The job may run exclusively in Agave, exclusively in the DE, or it
   may have steps that run on both systems."
  [agave submission]
  (let [app-id (:app_id submission)]
    (if (util/is-uuid? app-id)
      (submit-de-job agave (uuids/uuidify app-id) submission)
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
        update-group  #(update-in % [:parameters] update-props)
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

(defn- status-follows?
  "Determines whether or not the new job status follows the old job status."
  [new-status old-status]
  (> (jp/job-status-order new-status) (jp/job-status-order old-status)))

(defn update-job-status
  "Updates the status of a job. The job may have multiple steps, so the overall job status is only
   changed when first step changes to any status up to Running, the last step changes to any status
   after Running, or the status of any step changes to Failed."
  [agave username job job-step status end-time]
  (let [{job-id :id}                      job
        {:keys [external-id step-number]} job-step
        max-step                          (jp/get-max-step-number job-id)
        first-step?                       (= step-number 1)
        last-step?                        (= step-number max-step)
        status                            (translate-job-status agave job-step status)]
    (if (mu/is-completed? (:status job))
      (log/warn (str "received a job status update for completed or canceled job, " job-id))
      (when (status-follows? status (:status job-step))
        (jp/update-job-step job-id external-id status end-time)
        (when (or (and first-step? (mu/not-completed? status))
                  (and last-step? (mu/is-completed? status))
                  (= status jp/failed-status))
          (jp/update-job job-id status end-time)
          (mu/send-job-status-notification job status end-time))
        (when (and (not last-step?) (= status jp/completed-status))
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
        submission       (add-mapped-inputs agave job submission app-steps mapped-inputs)]
    (try+
     (submit-job-step agave job next-step submission)
     (catch Object o
       (log/warn (str "unable to submit the next step in job " (:id job)))
       (update-job-status agave username job next-step jp/failed-status (db/now))
       (throw+)))))

(defn- find-incomplete-job-steps
  "Finds the list of incomplete job steps associated with a job. An empty list is returned if the
   job has no incomplete steps."
  [job-id]
  (remove (comp mu/is-completed? :status) (jp/list-job-steps job-id)))

(defn- get-job-step-status
  [agave {:keys [job-type external-id]}]
  (if (= job-type jp/agave-job-type)
    (select-keys (.listJob agave external-id) [:status :enddate])
    (da/get-job-step-status external-id)))

(defn- sync-incomplete-job-status
  "Synchronizes the status of a job for which an incomplete step was found."
  [agave {:keys [id username] :as job} step]
  (if-let [step-status (get-job-step-status agave step)]
    (let [step     (jp/lock-job-step id (:external-id step))
          job      (jp/lock-job id)
          status   (:status step-status)
          end-date (db/timestamp-from-str (:enddate step-status))]
      (update-job-status agave username job step status end-date))
    (let [job (jp/lock-job id)]
      (update-job-status agave username job step jp/failed-status (db/now)))))

(defn- determine-job-status
  "Determines the status of a job for synchronization in the case when all job steps are
   marked as being in one of the completed statuses but the job itself is not."
  [job-id]
  (let [statuses (map :status (jp/list-job-steps job-id))
        status   (first (filter (partial not= jp/completed-status) statuses))]
    (cond (nil? status)                 jp/completed-status
          (= jp/canceled-status status) status
          (= jp/failed-status status)   status
          :else                         jp/failed-status)))

(defn- sync-complete-job-status
  "Synchronizes the status of a job for which an incomplete step was not found."
  [{:keys [id]}]
  (let [{:keys [status]} (jp/lock-job id)]
    (when-not (mu/is-completed? status)
      (jp/update-job id {:status (determine-job-status id) :end-date (db/now)}))))

(defn sync-job-status
  "Synchronizes the status of a job with the remote system."
  [agave {:keys [id username] :as job}]
  (if-let [step (first (find-incomplete-job-steps id))]
    (sync-incomplete-job-status agave job step)
    (sync-complete-job-status job)))

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
