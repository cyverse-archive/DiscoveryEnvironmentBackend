(ns metadactyl.metadata.job-view
  (:use [korma.core]
        [kameleon.core]
        [kameleon.entities])
  (:require [metadactyl.metadata.params :as mp]))

;; TODO:
;; * Add code to format parameters; metadactyl.zoidberg can serve as an example.
;; * Split the parameter formatting code into a shared namespace if it makes sense.
;; * Add code to omit mapped input parameters.
;; * Add code to omit implicit output parameters.
;; * Review the translation code to make sure nothing was missed.

(defn- get-parameters
  [group-id]
  (select (mp/params-base-query)
          (where {:p.parameter_group_id group-id
                  :p.is_visible         true})))

(defn- format-parameter
  [step parameter]
  (let [values (mp/get-param-values (:id parameter))
        type   (:type parameter)]
    {:arguments    (mp/format-param-values type values)
     :defaultValue ""
     :description  (:description parameter)
     :id           (str (:id step) "_" (:id parameter))
     :isVisible    (:is_visible parameter)
     :label        (:label parameter)
     :name         (:name parameter)
     :required     (:required parameter)
     :type         (:type parameter)
     :validators   []}))

(defn- get-groups
  [step-id]
  (select [:parameter_groups :g]
          (join :inner [:tasks :t] {:g.task_id :t.id})
          (join :inner [:app_steps :s] {:t.id :s.task_id})
          (fields :g.id :g.label)
          (where {:s.id step-id})))

(defn- format-group
  [name-prefix step group]
  {:id          (:id group)
   :label       (str name-prefix (:label group))
   :parameters  (mapv (partial format-parameter step) (get-parameters (:id group)))
   :step_number (:step_number step)})

(defn- format-groups
  [name-prefix step]
  (mapv (partial format-group name-prefix step) (get-groups (:id step))))

(defn- get-steps
  [app-id]
  (select [:app_steps :s]
          (join :inner [:tasks :t] {:s.task_id :t.id})
          (fields :s.id [:s.step :step_number] :s.task_id [:t.name :task_name])
          (where {:app_id app-id})))

(defn- format-steps
  [app-id]
  (let [app-steps         (get-steps app-id)
        multistep?        (> (count app-steps) 1)
        group-name-prefix (fn [{task-name :task_name}] (if multistep? (str task-name " - ") ""))]
    (doall (mapcat (fn [step] (format-groups (group-name-prefix step) step)) app-steps))))

(defn- format-app
  [app]
  {:id       (:id app)
   :name     (:name app)
   :type     (:overall_job_type app)
   :disabled (:disabled app)
   :groups   (format-steps (:id app))})

(defn get-app
  "This service obtains an app description in a format that is suitable for building the job
  submission UI."
  [app-id]
  (-> (amp/get-full-app app-id)
      (format-app)))
