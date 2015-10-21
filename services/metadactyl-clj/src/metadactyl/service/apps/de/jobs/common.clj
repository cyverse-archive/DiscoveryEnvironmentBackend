(ns metadactyl.service.apps.de.jobs.common
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.queries :only [get-user-id]]
        [kameleon.uuids :only [uuid]]
        [korma.core :exclude [update]]
        [medley.core :only [remove-vals]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [clojure.string :as string]
            [metadactyl.containers :as c]
            [metadactyl.service.apps.de.jobs.params :as params]
            [metadactyl.service.apps.de.jobs.util :as util]))

(defn- format-io-map
  [mapping]
  [(util/qual-id (:target_step mapping) (:input mapping))
   (util/qual-id (:source_step mapping) (:output mapping))])

(defn load-io-maps
  [app-id]
  (->> (select [:workflow_io_maps :wim]
               (join [:input_output_mapping :iom] {:wim.id :iom.mapping_id})
               (fields :wim.source_step :iom.output :wim.target_step :iom.input)
               (where {:wim.app_id          app-id
                       :iom.external_input  nil
                       :iom.external_output nil}))
       (map format-io-map)
       (into {})))

(defn build-default-values-map
  [params]
  (remove-vals #(string/blank? (str %))
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

(defn- add-container-info
  [{tool-id :id :as component}]
  (dissoc
   (if (c/tool-has-settings? tool-id)
     (assoc component :container (c/tool-container-info tool-id))
     component)
   :id))

(defn- load-step-component
  [task-id]
  (-> (select* :tasks)
      (join :tools {:tasks.tool_id :tools.id})
      (join :tool_types {:tools.tool_type_id :tool_types.id})
      (fields :tools.description :tools.location :tools.name [:tool_types.name :type] :tools.id)
      (where {:tasks.id task-id})
      (select)
      (first)
      (add-container-info)
      (remove-nil-vals)))

(defn build-component
  [{task-id :task_id}]
  (assert-not-nil [:tool-for-task task-id] (load-step-component task-id)))

(defn build-step
  [request-builder steps step]
  (let [config  (.buildConfig request-builder steps step)
        stdout  (:stdout config)
        stderr  (:stderr config)]
    (conj steps
          (remove-nil-vals
           {:component   (.buildComponent request-builder step)
            :environment (.buildEnvironment request-builder step)
            :config      (dissoc config :stdout :stderr)
            :stdout      stdout
            :stderr      stderr
            :type        "condor"}))))

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
       (reduce #(.buildStep request-builder %1 %2) [])))

(defn build-submission
  [request-builder user email submission app]
  {:app_description      (:description app)
   :app_id               (:id app)
   :app_name             (:name app)
   :archive_logs         (:archive_logs submission)
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
   :username             (:shortUsername user)
   :user_id              (get-user-id (:username user))
   :uuid                 (or (:uuid submission) (uuid))
   :wiki_url             (:wiki_url app)
   :skip-parent-meta     (:skip-parent-meta submission)
   :file-metadata        (:file-metadata submission)})
