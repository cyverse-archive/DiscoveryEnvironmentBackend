(ns metadactyl.zoidberg.pipeline-edit
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.app-groups :only [add-app-to-group get-app-subcategory-id]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [metadactyl.persistence.app-metadata :only [add-app
                                                    add-mapping
                                                    add-step
                                                    get-app
                                                    remove-app-steps
                                                    update-app]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.config :only [workspace-dev-app-group-index]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [metadactyl.validation :only [verify-app-editable verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]])
  (:require [metadactyl.util.service :as service]))

(def ^:private copy-prefix "Copy of ")

(def ^:private max-app-name-len 255)

(defn- name-too-long?
  "Determines if a name is too long to be extended for a copy name."
  [original-name]
  (> (+ (count copy-prefix) (count original-name)) max-app-name-len))

(defn- already-copy-name?
  "Determines if the name of an app is already a copy name."
  [original-name]
  (.startsWith original-name copy-prefix))

(defn- app-copy-name
  "Determines the name of a copy of an app."
  [original-name]
  (cond (name-too-long? original-name)     original-name
        (already-copy-name? original-name) original-name
        :else                              (str copy-prefix original-name)))

(defn- with-task-params
  "Includes a list of related file parameters in the query's result set,
   with fields required by the client."
  [query task-param-entity]
  (with query task-param-entity
    (join data_formats {:data_format :data_formats.id})
    (join :parameter_values {:parameter_values.parameter_id :id})
    (fields :id
            :name
            :label
            :description
            :required
            :parameter_values.value
            [:data_formats.name :format])))

(defn- get-tasks
  "Fetches a list of tasks for the given IDs with their inputs and outputs."
  [task-ids]
  (select tasks
          (fields :id
                  :name
                  :description)
          (with-task-params inputs)
          (with-task-params outputs)
          (where (in :id task-ids))))

(defn- format-task
  [task]
  (-> task
      (update-in [:inputs] (partial map remove-nil-vals))
      (update-in [:outputs] (partial map remove-nil-vals))))

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

(defn- format-workflow-app
  "Adds the steps and mappings fields to the app."
  [app]
  (let [steps (get-steps (:id app))
        mappings (get-mappings steps)
        steps (map format-step steps)]
    (-> app
        (assoc :steps steps)
        (assoc :mappings mappings)
        (dissoc :integrator_email
                :step_count))))

(defn- format-workflow
  "Prepares a JSON response for editing a Workflow in the client."
  [app]
  (let [app (format-workflow-app app)
        task-ids (set (map :task_id (:steps app)))
        tasks (map format-task (get-tasks task-ids))]
    {:apps [app]
     :tasks tasks}))

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
    (let [app-id (:id (add-app app))
          workspace-category-id (:root_category_id (get-workspace))
          dev-group-id (get-app-subcategory-id workspace-category-id (workspace-dev-app-group-index))]
      (add-app-to-group dev-group-id app-id)
      (add-app-steps-mappings app)
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
  (let [app-ids (map add-pipeline-app (:apps workflow))]
    {:apps app-ids}))

(defn update-pipeline
  [workflow]
  (let [app-ids (map update-pipeline-app (:apps workflow))]
    {:apps app-ids}))

(defn copy-pipeline
  "This service makes a copy of a Pipeline for the current user and returns the JSON for editing the
   copy in the client."
  [app-id]
  (let [app (get-app app-id)
        app (convert-app-to-copy app)
        app-id (add-pipeline-app app)]
    (edit-pipeline app-id)))
