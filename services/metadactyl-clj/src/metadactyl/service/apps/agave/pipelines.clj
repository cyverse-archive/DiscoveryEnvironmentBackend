(ns metadactyl.service.apps.agave.pipelines
  (:require [metadactyl.util.service :as service]))

(defn- get-agave-task
  [agave external-app-id]
  ((comp first :tasks)
   (service/assert-found (.listAppTasks agave external-app-id) "Agave app" external-app-id)))

(defn- format-task
  [agave external-app-ids {:keys [id] :as task}]
  (if-let [external-app-id (external-app-ids id)]
    (merge task (select-keys (get-agave-task agave external-app-id) [:inputs :outputs]))
    task))

(defn format-pipeline-tasks
  [agave pipeline]
  (let [external-app-ids (into {} (map (juxt :task_id :external_app_id) (:steps pipeline)))]
    (update-in pipeline [:tasks] (partial map (partial format-task agave external-app-ids)))))
