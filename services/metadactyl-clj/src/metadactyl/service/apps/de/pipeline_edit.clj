(ns metadactyl.service.apps.de.pipeline-edit
  (:use [korma.core :exclude [update]]
        [korma.db :only [transaction]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuidify]]
        [medley.core :only [find-first]]
        [metadactyl.persistence.app-metadata :only [add-app
                                                    add-mapping
                                                    add-step
                                                    add-task
                                                    get-app
                                                    remove-app-steps
                                                    update-app]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [metadactyl.validation :only [validate-external-app-step
                                      validate-pipeline
                                      verify-app-editable]]
        [metadactyl.service.apps.de.edit :only [add-app-to-user-dev-category app-copy-name]])
  (:require [metadactyl.service.apps.de.listings :as listings]))

(defn- add-app-type
  [step]
  (assoc step :app_type (if (:external_app_id step) "External" "DE")))

(defn- fix-task-id
  [step]
  (assoc step
    :task_id (first (remove nil? ((juxt :task_id :external_app_id) step)))))

(defn- get-steps
  "Fetches the steps for the given app ID, including their task ID and
   source/target mapping IDs and step names."
  [app-id]
  (select app_steps
    (with input_mapping
          (fields :source_step
                  :target_step)
          (group :source_step
                 :target_step))
    (join [:tasks :t]
          {:task_id :t.id})
    (join [:apps :app]
          {:app_id :app.id})
    (fields :app_steps.id
            :step
            :t.name
            :t.description
            :task_id
            :t.external_app_id)
    (where {:app.id app-id})
    (order :step :ASC)))

(defn- format-step
  "Formats step fields for the client."
  [step]
  (-> step
      add-app-type
      fix-task-id
      (dissoc :id :step :input_mapping)
      remove-nil-vals))

(defn- get-input-output-mappings
  "Fetches the output->input mapping UUIDs for the given source and target IDs."
  [source target]
  (select input_mapping
          (join [:input_output_mapping :map]
                {:id :map.mapping_id})
          (fields :map.input
                  :map.external_input
                  :map.output
                  :map.external_output)
          (where {:source_step source
                  :target_step target})))

(defn- find-first-key
  [pred m ks]
  (find-first pred ((apply juxt ks) m)))

(defn- format-io-mapping
  [m]
  (let [first-defined (partial find-first-key (complement nil?))]
    (vector (str (first-defined m [:input :external_input]))
            (str (first-defined m [:output :external_output])))))

(defn- format-mapping
  "Formats mapping fields for the client."
  [step-indexes {source-step-id :source_step target-step-id :target_step}]
  (let [input-output-mappings (get-input-output-mappings source-step-id target-step-id)]
    {:source_step (step-indexes source-step-id)
     :target_step (step-indexes target-step-id)
     :map         (into {} (map format-io-mapping input-output-mappings))}))

(defn- get-formatted-mapping
  "Formats a step's list of mapping IDs and step names to fields for the client."
  [step-indexes step]
  (map (partial format-mapping step-indexes) (:input_mapping step)))

(defn- get-mappings
  "Gets a list of formatted mappings for the given steps."
  [steps]
  (let [step-indexes (into {} (map #(vector (:id %) (:step %)) steps))]
    (mapcat (partial get-formatted-mapping step-indexes) steps)))

(defn- format-workflow
  "Prepares a JSON response for editing a Workflow in the client."
  [app]
  (let [steps (get-steps (:id app))
        mappings (get-mappings steps)
        task-ids (set (map :task_id steps))
        tasks (listings/get-tasks-with-file-params task-ids)
        steps (map format-step steps)]
    (-> app
        (select-keys [:id :name :description])
        (assoc :tasks tasks
               :steps steps
               :mappings mappings))))

(defn- convert-app-to-copy
  "Adds copies of the steps and mappings fields to the app, and formats
   appropriate app fields to prepare it for saving as a copy."
  [app]
  (let [steps (get-steps (:id app))
        mappings (get-mappings steps)
        steps (map format-step steps)]
    (-> app
        (select-keys [:description])
        (assoc :name (app-copy-name (:name app)))
        (assoc :steps steps)
        (assoc :mappings mappings))))

(defn edit-pipeline
  "This service prepares a JSON response for editing a Pipeline in the client."
  [user app-id]
  (let [app (get-app app-id)]
    (verify-app-editable user app)
    (format-workflow app)))

(defn- add-app-mapping
  [app-id steps {:keys [source_step target_step map] :as mapping}]
  (add-mapping {:app_id app-id
                :source_step (nth steps source_step)
                :target_step (nth steps target_step)
                :map map}))

(defn- generate-external-app-task
  [step]
  {:name            (:name step)
   :description     (:description step)
   :label           (:name step)
   :external_app_id (:external_app_id step)})

(defn- add-external-app-task
  [step-number step]
  (validate-external-app-step step-number step)
  (-> step generate-external-app-task add-task))

(defn- add-pipeline-step
  [app-id step-number step]
  (if (nil? (:task_id step))
    (let [task-id (:id (add-external-app-task step-number step))]
      (add-step app-id step-number (assoc step :task_id task-id)))
    (add-step app-id step-number step)))

(defn- add-pipeline-steps
  [app-id steps]
  "Adds steps to a pipeline. The app type isn't stored in the database, but needs to be kept in
   the list of steps so that external steps can be distinguished from DE steps. The two types of
   steps normally can't be distinguished without examining the associated task."
  (doall
    (map-indexed (fn [step-number step]
                   (assoc (add-pipeline-step app-id step-number step)
                     :app_type (:app_type step)))
                 steps)))

(defn- add-app-steps-mappings
  [{app-id :id steps :steps mappings :mappings}]
  (let [steps (add-pipeline-steps app-id steps)]
    (dorun (map (partial add-app-mapping app-id steps) mappings))))

(defn- add-pipeline-app
  [user app]
  (validate-pipeline app)
  (transaction
    (let [app-id (:id (add-app app))]
      (add-app-to-user-dev-category user app-id)
      (add-app-steps-mappings (assoc app :id app-id))
      app-id)))

(defn- update-pipeline-app
  [user app]
  (validate-pipeline app)
  (transaction
    (let [app-id (:id app)]
      (verify-app-editable user (get-app app-id))
      (update-app app)
      (remove-app-steps app-id)
      (add-app-steps-mappings app)
      app-id)))

(defn- prepare-pipeline-step
  "Prepares a single step in a pipeline for submission to metadactyl. DE steps can be left as-is.
   External steps need to have the task_id field moved to the external_app_id field."
  [{app-type :app_type :as step}]
  (if (= app-type "External")
    (assoc (dissoc step :task_id) :external_app_id (:task_id step))
    (update-in step [:task_id] uuidify)))

(defn- preprocess-pipeline
  [pipeline]
  (update-in pipeline [:steps] (partial map prepare-pipeline-step)))

(defn add-pipeline
  [user workflow]
  (->> (preprocess-pipeline workflow)
       (add-pipeline-app user)
       (edit-pipeline user)))

(defn update-pipeline
  [user workflow]
  (let [app-id (update-pipeline-app user (preprocess-pipeline workflow))]
    (edit-pipeline user app-id)))

(defn copy-pipeline
  "This service makes a copy of a Pipeline for the current user and returns the JSON for editing the
   copy in the client."
  [user app-id]
  (->> (get-app app-id)
       (convert-app-to-copy)
       (add-pipeline-app user)
       (edit-pipeline user)))
