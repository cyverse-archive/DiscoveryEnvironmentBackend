(ns metadactyl.analyses.common
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [korma.core]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [clojure.string :as string]
            [metadactyl.analyses.params :as params]
            [metadactyl.analyses.util :as util]))

(defn- format-io-map
  [mapping]
  [(util/qual-id (:target_step mapping) (:input mapping))
   (util/qual-id (:source_step mapping) (:output mapping))])

(defn load-io-maps
  [app-id]
  (->> (select [:workflow_io_maps :wim]
               (join [:input_output_mapping :iom] {:wim.id :iom.mapping_id})
               (fields :wim.source_step :iom.output :wim.target_step :iom.input)
               (where {:wim.app_id app-id}))
       (map format-io-map)
       (into {})))

(defn build-default-values-map
  [params]
  (remove-nil-vals
    (into {} (map (juxt util/param->qual-id :default_value) params))))

(defn build-config
  [inputs outputs params]
  {:input  inputs
   :output outputs
   :params params})

(defn- build-environment-entries
  [config default-values param]
  (let [value (params/value-for-param config default-values param)]
    (if (or (util/not-blank? value) (not (:omit_if_blank param)))
      [[(:name param) value]]
      [])))

(defn build-environment
  [config default-values params]
  (->> (filter #(= (:type %) util/environment-variable-type) params)
       (mapcat (partial build-environment-entries config default-values))
       (into {})))

(defn- load-step-component
  [task-id]
  (-> (select* :tasks)
      (join :tools {:tasks.tool_id :tools.id})
      (join :tool_types {:tools.tool_type_id :tool_types.id})
      (fields :tools.description :tools.location :tools.name [:tool_types.name :type])
      (where {:tasks.id task-id})
      (select)
      (first)))

(defn build-component
  [{task-id :task_id}]
  (assert-not-nil [:tool-for-task task-id] (load-step-component task-id)))

(defn build-step
  [request-builder step]
  {:component   (.buildComponent request-builder step)
   :config      (.buildConfig request-builder step)
   :environment (.buildEnvironment request-builder step)
   :type        "condor"})

(defn load-steps
  [app-id]
  (select [:app_steps :s]
          (join [:tasks :t] {:s.task_id :t.id})
          (fields [:s.id              :id]
                  [:s.step            :step]
                  [:s.task_id         :task_id]
                  [:t.external_app_id :external_app_id])
          (where {:s.app_id app-id})
          (order :s.step)))

(defn build-steps
  [request-builder app submission]
  (->> (load-steps (:id app))
       (drop (dec (:starting_step submission 1)))
       (take-while (comp nil? :external_app_id))
       (mapv #(.buildStep request-builder %))))

(defn build-submission
  [request-builder user email submission app]
  {:app_description      (:description app)
   :app_id               (:id app)
   :app_name             (:name app)
   :callback             (:callback submission)
   :create_output_subdir (:create_output_subdir submission true)
   :description          (:description submission "")
   :email                email
   :execution_target     "condor"
   :name                 (:name submission)
   :notify               (:notify submission)
   :output_dir           (:output_dir submission)
   :request_type         "submit"
   :steps                (.buildSteps request-builder)
   :username             user
   :uuid                 (or (:uuid submission) (uuid))
   :wiki_url             (:wiki_url app)
   :skip-parent-meta     (:skip-parent-meta submission)
   :file-metadata        (:file-metadata submission)})
