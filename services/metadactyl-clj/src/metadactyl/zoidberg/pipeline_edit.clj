(ns metadactyl.zoidberg.pipeline-edit
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [metadactyl.app-listings :only [get-tasks-with-file-params]]
        [metadactyl.persistence.app-metadata :only [add-app
                                                    add-mapping
                                                    add-step
                                                    get-app
                                                    remove-app-steps
                                                    update-app]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [metadactyl.validation :only [verify-app-editable verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]]
        [metadactyl.zoidberg.app-edit :only [add-app-to-user-dev-category app-copy-name]])
  (:require [metadactyl.util.service :as service]))

(defn- add-app-type
  [step]
  (assoc step :app_type (if (:external_app_id step) "External" "DE")))

(defn- fix-task-id
  [step]
  (-> step
      (assoc :task_id (first (remove nil? ((juxt :task_id :external_app_id) step))))
      (dissoc :external_app_id)))

(defn- get-steps
  "Fetches the steps for the given app ID, including their task ID and
   source/target mapping IDs and step names."
  [app-id]
  (map (comp fix-task-id add-app-type)
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
            (order :step :ASC))))

(defn- format-step
  "Formats step fields for the client."
  [step]
  (dissoc step :id :step :input_mapping))

(defn- get-input-output-mappings
  "Fetches the output->input mapping UUIDs for the given source and target IDs."
  [source target]
  (select input_mapping
          (join [:input_output_mapping :map]
                {:id :map.mapping_id})
          (fields :map.input
                  :map.output)
          (where {:source_step source
                  :target_step target})))

(defn- format-mapping
  "Formats mapping fields for the client."
  [step-indexes {source-step-id :source_step target-step-id :target_step}]
  (let [input-output-mappings (get-input-output-mappings source-step-id target-step-id)
        input-output-reducer #(assoc %1 (:input %2) (:output %2))]
    {:source_step (step-indexes source-step-id)
     :target_step (step-indexes target-step-id)
     :map (reduce input-output-reducer {} input-output-mappings)}))

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
        tasks (get-tasks-with-file-params task-ids)
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
  [app-id]
  (let [app (get-app app-id)]
    (verify-app-editable app)
    (service/success-response (format-workflow app))))

(defn- add-app-mapping
  [app-id steps {:keys [source_step target_step map]}]
  (add-mapping {:app_id app-id
                :source_step (:id (nth steps source_step))
                :target_step (:id (nth steps target_step))
                :map map}))

(defn- add-app-steps-mappings
  [{app-id :id steps :steps mappings :mappings}]
  (let [steps (map-indexed (partial add-step app-id) steps)]
    (dorun (map (partial add-app-mapping app-id steps) mappings))))

(defn- add-pipeline-app
  [app]
  (transaction
    (let [app-id (:id (add-app app))]
      (add-app-to-user-dev-category app-id)
      (add-app-steps-mappings (assoc app :id app-id))
      app-id)))

(defn- update-pipeline-app
  [app]
  (transaction
    (let [app-id (:id app)]
      (verify-app-editable (get-app app-id))
      (update-app app)
      (remove-app-steps app-id)
      (add-app-steps-mappings app)
      app-id)))

(defn add-pipeline
  [workflow]
  (let [app-id (add-pipeline-app workflow)]
    (edit-pipeline app-id)))

(defn update-pipeline
  [workflow]
  (let [app-id (update-pipeline-app workflow)]
    (edit-pipeline app-id)))

(defn copy-pipeline
  "This service makes a copy of a Pipeline for the current user and returns the JSON for editing the
   copy in the client."
  [app-id]
  (let [app (get-app app-id)
        app (convert-app-to-copy app)
        app-id (add-pipeline-app app)]
    (edit-pipeline app-id)))
