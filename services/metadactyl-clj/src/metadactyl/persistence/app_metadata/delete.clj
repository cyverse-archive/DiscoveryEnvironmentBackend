(ns metadactyl.persistence.app-metadata.delete
  "Functions used to remove apps from the database."
  (:use [metadactyl.user :only [current-user]]
        [kameleon.entities]
        [korma.core :exclude [update]]
        [korma.db :only [transaction]])
  (:require [clojure.tools.logging :as log]))

(defn- tasks-for-app
  "Loads the list of tasks associated with an app."
  [app-id]
  (select tasks
          (join [:app_steps :step]
                {:step.task_id :tasks.id})
          (where {:step.app_id app-id})))

(defn- task-orphaned?
  "Determines whether or not a task is orphaned."
  [task-id]
  ((comp zero? :count first)
   (select :app_steps
           (aggregate (count :*) :count)
           (where {:task_id task-id}))))

(defn- delete-orphaned-task
  "Deletes a task if it's orphaned (that is, if it's not used in any app). Task deletes should
   cascade to parameter groups, parameters, and other parameter related tables."
  [task-id]
  (when (task-orphaned? task-id)
    (delete tasks (where {:id task-id}))))

(defn- remove-app
  "Removes an app from the database. App deletes should cascade to app related tables like ratings,
   references, steps, and mappings."
  [app-id]
  (delete apps (where {:id app-id})))

(defn permanently-delete-app
  "Permanently removes an app from the database."
  [app-id]
  (transaction
    (let [tasks    (tasks-for-app app-id)
          task-ids (mapv :id tasks)]
      (log/warn (:username current-user) "permanently deleting App" app-id)
      (remove-app app-id)
      (dorun (map delete-orphaned-task task-ids)))))
