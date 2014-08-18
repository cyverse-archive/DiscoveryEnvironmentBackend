(ns metadactyl.zoidberg
  (:use [korma.core]
        [kameleon.core]
        [kameleon.entities]
        [metadactyl.user :only [current-user]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.error-codes :as cc-errs]))

(defn- get-implementor-details
  "Gets an implementor object with details from the current-user, needed to save
   workflows."
  []
  {:implementor       (str (:first-name current-user) " " (:last-name current-user))
   :implementor_email (:email current-user)
   :test              {:params []}})

(defn- verify-ownership
  "Verifies that the current user owns the app that is being edited."
  [app]
  (let [owner (:integrator_email app)]
    (if (not= owner (:email current-user))
      (throw+ {:code cc-errs/ERR_NOT_OWNER,
               :username (:username current-user),
               :message (str
                          (:shortUsername current-user)
                          " does not own app "
                          (:analysis_id app))}))))

(defn- verify-app-not-public
  "Verifies that an app has not been made public."
  [app]
  (if (:is_public app)
    (throw+ {:code cc-errs/ERR_NOT_WRITEABLE,
             :message (str "Workflow, "
                           (:analysis_id app)
                           ", is public and may not be edited")})))

(defn- verify-app-editable
  "Verifies that the app is allowed to be edited by the current user."
  [app]
  (verify-ownership app)
  (verify-app-not-public app))

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
                   :t.name
                   :t.description
                   :task_id
                   :t.external_app_id)
           (where {:app.id app-id}))))

(defn- format-step
  "Formats step fields for the client."
  [step]
  (dissoc step :input_mapping))

(defn- format-step-copy
  "Formats step fields as copies for an update-workflow call."
  [step]
  (-> step
    (dissoc :id)
    (dissoc :guid)
    (dissoc :input_mapping)
    (assoc :config {})))

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
  [mapping]
  (let [input-output-mappings (get-input-output-mappings (:source_step mapping)
                                                         (:target_step mapping))
        input-output-reducer #(assoc %1 (:input %2) (:output %2))]
    (assoc mapping :map (reduce input-output-reducer {} input-output-mappings))))

(defn- get-formatted-mapping
  "Formats a step's list of mapping IDs and step names to fields for the client."
  [step]
  (map format-mapping (:input_mapping step)))

(defn- format-app
  "Adds the steps and mappings fields to the app, and extracts a set of
   task IDs from the steps into a tasks field."
  [app]
  (let [steps (get-steps (:analysis_id app))
        task-ids (set (map :task_id steps))
        mappings (mapcat get-formatted-mapping steps)
        steps (map format-step steps)]
    (-> app
      (assoc :steps steps)
      (assoc :mappings mappings)
      (assoc :tasks task-ids))))

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

(defn- convert-app-to-copy
  "Adds copies of the steps and mappings fields to the app, and formats
   appropriate app fields to prepare it for saving as a copy."
  [app]
  (let [steps (get-steps (:analysis_id app))
        mappings (mapcat get-formatted-mapping steps)
        steps (map format-step-copy steps)]
    (-> app
      (dissoc :integration_data_id)
      (assoc :analysis_id "auto-gen")
      (assoc :analysis_name (app-copy-name (:analysis_name app)))
      (assoc :implementation (get-implementor-details))
      (assoc :full_username (:username current-user))
      (assoc :steps steps)
      (assoc :mappings mappings))))

(defn- get-app
  "Fetches an app with the given ID."
  [app-id]
  (let [app (select app_listing
                         (fields [:id :analysis_id]
                                 [:name :analysis_name]
                                 :description
                                 :integrator_name
                                 :integrator_email)
                         (where {:id app-id}))
        app (first app)]
    (when (empty? app)
      (throw+ {:code cc-errs/ERR_DOES_NOT_EXIST,
               :message (str "App, " app-id ", not found")}))
    app))

(defn edit-app
  "This service prepares a JSON response for editing an App in the client."
  [app-id]
  (let [app (get-app app-id)
        _  (verify-app-editable app)
        app (format-app app)
        task-ids (:tasks app)
        tasks (get-tasks task-ids)
        app (dissoc app :tasks)]
    (cheshire/encode {:analyses [app]
                      :templates tasks})))

;; FIXME
(defn copy-app
  "This service makes a copy of an App available in Tito for editing."
  [app-id]
  (let [app (get-app app-id)
        app (convert-app-to-copy app)
        app-json (cheshire/encode {:analyses [app]})
        update-response (throw+ '("update-app-from-json" app-json))
        app-copy (cheshire/decode update-response true)
        app-id (first (:analyses app-copy))]
    (edit-app app-id)))
