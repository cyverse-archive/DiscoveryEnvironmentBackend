(ns facepalm.c197-2015042101
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "1.9.7:20150421.01")

(defn- sync-task-names-descriptions
  []
  (println "\t* setting task names and descriptions to their single-step app names and descriptions")
  (exec-sql-statement
    "UPDATE tasks t SET name = a.name, description = a.description
     FROM app_listing a
     JOIN app_steps step ON step.app_id = a.id
     WHERE t.id = step.task_id
     AND a.step_count = 1
     AND (a.name != t.name OR a.description != t.description)"))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (sync-task-names-descriptions))
