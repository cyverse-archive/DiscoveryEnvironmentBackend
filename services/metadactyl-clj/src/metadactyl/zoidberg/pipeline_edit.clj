(ns metadactyl.zoidberg.pipeline-edit
  (:use [korma.core]
        [korma.db :only [transaction]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [medley.core :only [find-first]]
        [metadactyl.app-listings :only [get-tasks-with-file-params]]
        [metadactyl.persistence.app-metadata :only [add-app
                                                    add-mapping
                                                    add-step
                                                    add-task
                                                    get-app
                                                    remove-app-steps
                                                    update-app]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [metadactyl.validation :only [validate-external-app-step
                                      validate-pipeline
                                      verify-app-editable
                                      verify-app-ownership]]
        [metadactyl.workspace :only [get-workspace]]
        [metadactyl.service.apps.de.edit :only [add-app-to-user-dev-category app-copy-name]])
  (:require [metadactyl.util.service :as service]
            [clojure.tools.logging :as log]))

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
        tasks (get-tasks-with-file-params task-ids)
        steps (map format-step steps)]
    (-> app
        (select-keys [:id :name :description])
        (assoc :tasks tasks
               :steps steps
               :mappings mappings))))

(defn edit-pipeline
  "This service prepares a JSON response for editing a Pipeline in the client."
  [app-id]
  (let [app (get-app app-id)]
    (verify-app-editable app)
    (service/success-response (format-workflow app))))
